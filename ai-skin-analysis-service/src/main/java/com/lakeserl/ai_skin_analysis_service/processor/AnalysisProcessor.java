package com.lakeserl.ai_skin_analysis_service.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lakeserl.ai_skin_analysis_service.ai.SkinAnalysisAiResult;
import com.lakeserl.ai_skin_analysis_service.client.ProductServiceClient;
import com.lakeserl.ai_skin_analysis_service.dto.response.RecommendedProductDTO;
import com.lakeserl.ai_skin_analysis_service.dto.response.RoutineDTO;
import com.lakeserl.ai_skin_analysis_service.entity.AIUsageLog;
import com.lakeserl.ai_skin_analysis_service.entity.SkinAnalysisSession;
import com.lakeserl.ai_skin_analysis_service.enums.AnalysisStatus;
import com.lakeserl.ai_skin_analysis_service.event.payload.SkinAnalysisCompletedEvent;
import com.lakeserl.ai_skin_analysis_service.event.producer.AIEventProducer;
import com.lakeserl.ai_skin_analysis_service.repository.AIUsageLogRepository;
import com.lakeserl.ai_skin_analysis_service.repository.SkinAnalysisSessionRepository;
import com.lakeserl.ai_skin_analysis_service.service.AIAnalysisService;
import com.lakeserl.ai_skin_analysis_service.service.CloudinaryService;
import com.lakeserl.ai_skin_analysis_service.service.ImagePreprocessingService;
import com.lakeserl.ai_skin_analysis_service.service.RoutineGeneratorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AnalysisProcessor {

    private final SkinAnalysisSessionRepository sessionRepository;
    private final AIUsageLogRepository usageLogRepository;
    private final ImagePreprocessingService preprocessingService;
    private final AIAnalysisService aiAnalysisService;
    private final CloudinaryService cloudinaryService;
    private final RoutineGeneratorService routineGeneratorService;
    private final ProductServiceClient productServiceClient;
    private final AIEventProducer eventProducer;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.ai.vision-model:gemini-3.5-flash}")
    private String modelName;

    /**
     * Accepts a temp-file path, NOT raw bytes.
     *
     * Why: byte[] captured in a Runnable closure pins heap memory for every task sitting
     * in the queue. 50 queued tasks × 10 MB = 500 MB heap permanently held before any
     * task runs. Passing a file path costs ~100 bytes per queued task.
     *
     * The caller writes imageBytes to a temp file and submits the path here.
     * This method reads the file immediately on entry and deletes it, so the temp
     * file lifetime is: [submission → task starts] plus a few milliseconds.
     */
    @Async("aiTaskExecutor")
    @Transactional
    public void processAsync(String sessionId, String tempImagePath, Long userId) {
        log.info("Starting async skin analysis for sessionId={}", sessionId);
        long startTime = System.currentTimeMillis();

        SkinAnalysisSession session = sessionRepository.findBySessionId(sessionId).orElse(null);
        if (session == null) {
            log.error("Session not found for async processing: {}", sessionId);
            deleteTempFile(tempImagePath);
            return;
        }

        // Read and immediately delete the temp file — it's only needed for the first few steps
        byte[] imageBytes = readAndDeleteTempFile(tempImagePath, sessionId);
        if (imageBytes == null) {
            session.setStatus(AnalysisStatus.FAILED);
            session.setFailureReason("Image buffer expired or corrupted before processing started");
            sessionRepository.save(session);
            releaseDedupKey(session.getImageHash());
            return;
        }

        try {
            // Step 1: Face detection
            boolean hasFace = preprocessingService.hasFace(imageBytes);
            if (!hasFace) {
                log.warn("No face detected in image for sessionId={}", sessionId);
                session.setStatus(AnalysisStatus.FAILED);
                session.setFailureReason("No face detected in the provided image. Please upload a clear face photo.");
                sessionRepository.save(session);
                // Dedup key stays set — same image would fail again; user must submit a new photo
                return;
            }

            // Step 2: Normalize image (EXIF-stripped, resized to 512×512)
            byte[] normalizedBytes = preprocessingService.normalize(imageBytes);
            // Release original bytes explicitly — GC hint; the reference leaves scope anyway
            imageBytes = null;

            // Step 3: Extract features
            String features = preprocessingService.extractFeatures(normalizedBytes);
            session.setCvFeatures(features);

            // Step 4: Upload ONLY the normalized image to Cloudinary.
            //   We do NOT upload the original. Reasons:
            //   (a) Original contains EXIF GPS metadata (PII under Decree 13/2023).
            //   (b) Only the processed image is needed for display; storing the raw
            //       biometric face photo long-term is unnecessary liability.
            String processedPublicId = sessionId + "-processed";
            cloudinaryService.uploadAsync(normalizedBytes, processedPublicId).thenAccept(url -> {
                if (url != null) {
                    sessionRepository.findBySessionId(sessionId).ifPresent(s -> {
                        s.setProcessedImageUrl(url);
                        sessionRepository.save(s);
                    });
                }
            }).exceptionally(t -> {
                log.warn("Cloudinary upload failed for sessionId={}: {}", sessionId, t.getMessage());
                return null;
            });

            // Step 5: Call AI analysis
            SkinAnalysisAiResult aiResult = aiAnalysisService.analyze(
                    normalizedBytes, features, session.getAge(),
                    session.getSelfSkinType(), session.getSelfConcerns());

            // Step 6: Log AI usage
            AIUsageLog usageLog = AIUsageLog.builder()
                    .userId(userId)
                    .serviceName("ai-skin-analysis-service")
                    .modelName(modelName)
                    .tokensInput(aiResult.getTokensInput() != null ? aiResult.getTokensInput() : 0)
                    .tokensOutput(aiResult.getTokensOutput() != null ? aiResult.getTokensOutput() : 0)
                    .costUsd(estimateCost(aiResult.getTokensInput(), aiResult.getTokensOutput()))
                    .responseTimeMs(aiResult.getResponseTimeMs() != null ? aiResult.getResponseTimeMs().intValue() : 0)
                    .success(true)
                    .build();
            usageLogRepository.save(usageLog);

            // Step 7: Get product recommendations
            List<RecommendedProductDTO> products = productServiceClient
                    .findBySkinProfile(aiResult.getDetectedSkinType(), aiResult.getConcerns());

            // Step 8: Generate routines
            RoutineDTO morningRoutine;
            RoutineDTO eveningRoutine;
            if (aiResult.getMorningSteps() != null && !aiResult.getMorningSteps().isEmpty()) {
                morningRoutine = RoutineDTO.builder().steps(aiResult.getMorningSteps()).build();
                eveningRoutine = RoutineDTO.builder().steps(
                        aiResult.getEveningSteps() != null ? aiResult.getEveningSteps() : List.of()).build();
            } else {
                morningRoutine = routineGeneratorService.generateMorningRoutine(
                        aiResult.getDetectedSkinType(), aiResult.getConcerns());
                eveningRoutine = routineGeneratorService.generateEveningRoutine(
                        aiResult.getDetectedSkinType(), aiResult.getConcerns());
            }

            // Step 9: Update session with all results
            long elapsedTotal = System.currentTimeMillis() - startTime;
            session.setDetectedSkinType(aiResult.getDetectedSkinType());
            session.setDetectedConcerns(toJson(aiResult.getConcerns()));
            session.setSkinConditionReport(aiResult.getAdvice());
            session.setRecommendedProductIds(toJson(products));
            session.setMorningRoutine(toJson(morningRoutine));
            session.setEveningRoutine(toJson(eveningRoutine));
            session.setTokensUsed((aiResult.getTokensInput() != null ? aiResult.getTokensInput() : 0)
                    + (aiResult.getTokensOutput() != null ? aiResult.getTokensOutput() : 0));
            session.setProcessingTimeMs((int) elapsedTotal);
            session.setStatus(AnalysisStatus.COMPLETED);
            session.setCompletedAt(LocalDateTime.now());
            sessionRepository.save(session);

            // Dedup key was already set via SET NX at submission — no write needed here.

            // Step 10: Publish Kafka event
            SkinAnalysisCompletedEvent event = SkinAnalysisCompletedEvent.builder()
                    .sessionId(sessionId)
                    .userId(userId)
                    .detectedSkinType(aiResult.getDetectedSkinType())
                    .concerns(aiResult.getConcerns())
                    .completedAt(session.getCompletedAt())
                    .build();
            eventProducer.publishSkinAnalysisCompleted(event);

            log.info("Skin analysis completed for sessionId={} in {}ms", sessionId, elapsedTotal);

        } catch (Exception e) {
            log.error("Async analysis failed for sessionId={}: {}", sessionId, e.getMessage(), e);

            try {
                AIUsageLog failLog = AIUsageLog.builder()
                        .userId(userId)
                        .serviceName("ai-skin-analysis-service")
                        .modelName(modelName)
                        .tokensInput(0)
                        .tokensOutput(0)
                        .success(false)
                        .errorMessage(e.getMessage())
                        .build();
                usageLogRepository.save(failLog);
            } catch (Exception logEx) {
                log.error("Could not save failure usage log: {}", logEx.getMessage());
            }

            session.setStatus(AnalysisStatus.FAILED);
            session.setFailureReason(e.getMessage());
            try {
                sessionRepository.save(session);
            } catch (Exception saveEx) {
                log.error("Could not save failed session state: {}", saveEx.getMessage());
            }

            // Release dedup guard on failure so the same image can be resubmitted
            releaseDedupKey(session.getImageHash());
        }
    }

    private byte[] readAndDeleteTempFile(String tempImagePath, String sessionId) {
        Path path = Path.of(tempImagePath);
        try {
            byte[] bytes = Files.readAllBytes(path);
            Files.deleteIfExists(path);
            return bytes;
        } catch (Exception e) {
            log.error("Failed to read temp image file for sessionId={}: {}", sessionId, e.getMessage());
            deleteTempFile(tempImagePath);
            return null;
        }
    }

    private void deleteTempFile(String tempImagePath) {
        try {
            Files.deleteIfExists(Path.of(tempImagePath));
        } catch (Exception e) {
            log.warn("Could not delete temp image file {}: {}", tempImagePath, e.getMessage());
        }
    }

    private void releaseDedupKey(String imageHash) {
        if (imageHash != null) {
            try {
                redisTemplate.delete("ai:skin:dedup:" + imageHash);
            } catch (Exception e) {
                log.warn("Could not release dedup key for hash={}: {}", imageHash, e.getMessage());
            }
        }
    }

    private BigDecimal estimateCost(Integer inputTokens, Integer outputTokens) {
        // gemini-3.5-flash pricing estimate: $0.0035/1k input, $0.0105/1k output
        int in = inputTokens != null ? inputTokens : 0;
        int out = outputTokens != null ? outputTokens : 0;
        double cost = (in / 1000.0 * 0.0035) + (out / 1000.0 * 0.0105);
        return BigDecimal.valueOf(cost);
    }

    private String toJson(Object obj) {
        if (obj == null) return null;
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            log.warn("Could not serialize object to JSON: {}", e.getMessage());
            return null;
        }
    }
}
