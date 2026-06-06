package com.lakeserl.ai_skin_analysis_service.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lakeserl.ai_skin_analysis_service.dto.response.RecommendedProductDTO;
import com.lakeserl.ai_skin_analysis_service.dto.response.RoutineDTO;
import com.lakeserl.ai_skin_analysis_service.dto.response.SkinAnalysisResultDTO;
import com.lakeserl.ai_skin_analysis_service.entity.SkinAnalysisSession;
import com.lakeserl.ai_skin_analysis_service.enums.AnalysisStatus;
import com.lakeserl.ai_skin_analysis_service.exception.AIServiceUnavailableException;
import com.lakeserl.ai_skin_analysis_service.exception.BiometricConsentMissingException;
import com.lakeserl.ai_skin_analysis_service.exception.DailyLimitExceededException;
import com.lakeserl.ai_skin_analysis_service.exception.ImageTooLargeException;
import com.lakeserl.ai_skin_analysis_service.exception.InvalidImageFormatException;
import com.lakeserl.ai_skin_analysis_service.exception.SessionNotFoundException;
import com.lakeserl.ai_skin_analysis_service.processor.AnalysisProcessor;
import com.lakeserl.ai_skin_analysis_service.repository.AIUsageLogRepository;
import com.lakeserl.ai_skin_analysis_service.repository.SkinAnalysisSessionRepository;
import com.lakeserl.ai_skin_analysis_service.service.CloudinaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class SkinAnalysisServiceImpl implements SkinAnalysisService {

    private static final Set<String> ALLOWED_MIME_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024L; // 10MB

    // Magic-byte signatures — don't trust Content-Type header alone
    private static final byte[] MAGIC_JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] MAGIC_PNG  = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
    private static final byte[] MAGIC_RIFF = {0x52, 0x49, 0x46, 0x46}; // WebP starts with RIFF
    private static final byte[] MAGIC_WEBP = {0x57, 0x45, 0x42, 0x50}; // bytes 8-11

    private final SkinAnalysisSessionRepository sessionRepository;
    private final AIUsageLogRepository usageLogRepository;
    private final AnalysisProcessor analysisProcessor;
    private final CloudinaryService cloudinaryService;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.ai.daily-limit-skin-analysis:10}")
    private int dailyLimit;

    @Value("${app.consent.version:1.0}")
    private String consentVersion;

    @Override
    @Transactional
    public Map<String, String> initiateAnalysis(MultipartFile image, Integer age, String selfSkinType,
                                                  String selfConcerns, String allergies,
                                                  boolean consentGiven, Long userId) {
        // 1. Consent check — face photos are biometric data under Decree 13/2023.
        //    Must be verified before any data is stored or transmitted to external APIs.
        if (!consentGiven) {
            throw new BiometricConsentMissingException(
                    "Explicit consent is required to collect and process your facial biometric data. "
                    + "Your image will be analyzed by Google Gemini API (outside Vietnam) and stored on Cloudinary.");
        }

        // 2. Validate MIME type
        String contentType = image.getContentType();
        if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType.toLowerCase())) {
            throw new InvalidImageFormatException(
                    "Unsupported image format: " + contentType + ". Allowed: jpeg, png, webp");
        }

        // 2. Validate file size
        if (image.getSize() > MAX_FILE_SIZE) {
            throw new ImageTooLargeException(
                    "Image size " + image.getSize() + " bytes exceeds maximum 10MB");
        }

        // 3. Read bytes
        byte[] imageBytes;
        try {
            imageBytes = image.getBytes();
        } catch (Exception e) {
            throw new InvalidImageFormatException("Could not read image data: " + e.getMessage());
        }

        // 4. Magic-byte validation — Content-Type header is user-supplied and untrusted
        validateMagicBytes(imageBytes, contentType);

        // 5. Compute SHA-256 hash
        String imageHash = computeSha256(imageBytes);

        // 6. Dedup guard (read): return early before touching the quota counter.
        //    A completed session's dedup key is set by AnalysisProcessor after success.
        String dedupKey = "ai:skin:dedup:" + imageHash;
        String cachedSessionId = redisTemplate.opsForValue().get(dedupKey);
        if (cachedSessionId != null) {
            log.info("Dedup hit for imageHash={}, returning sessionId={}", imageHash, cachedSessionId);
            return Map.of("sessionId", cachedSessionId);
        }

        // 7. Check daily limit (INCR + EXPIREAT)
        String today = LocalDate.now().toString();
        String limitKey = "ai:skin:limit:" + userId + ":" + today;
        Long currentCount = redisTemplate.opsForValue().increment(limitKey);
        if (currentCount == 1L) {
            LocalDateTime endOfDay = LocalDate.now().atTime(23, 59, 59);
            long epochSeconds = endOfDay.atZone(ZoneId.systemDefault()).toEpochSecond();
            redisTemplate.expireAt(limitKey, new java.util.Date(epochSeconds * 1000));
        }
        if (currentCount != null && currentCount > dailyLimit) {
            throw new DailyLimitExceededException(
                    "Daily skin analysis limit of " + dailyLimit + " reached. Try again tomorrow.");
        }

        // 8. Generate session ID and atomically claim the dedup slot (SET NX).
        //    Closes the race window where two concurrent requests for the same image
        //    both pass the GET check above and both start full processing.
        String sessionId = UUID.randomUUID().toString();
        Boolean claimed = redisTemplate.opsForValue().setIfAbsent(dedupKey, sessionId, 24, TimeUnit.HOURS);
        if (Boolean.FALSE.equals(claimed)) {
            // A concurrent submission won the race — undo our quota increment and return their sessionId
            redisTemplate.opsForValue().decrement(limitKey);
            String concurrentSessionId = redisTemplate.opsForValue().get(dedupKey);
            log.info("Dedup NX race for imageHash={}, deferring to concurrent sessionId={}", imageHash, concurrentSessionId);
            return Map.of("sessionId", concurrentSessionId != null ? concurrentSessionId : sessionId);
        }

        // 9. Create and save session entity
        SkinAnalysisSession session = SkinAnalysisSession.builder()
                .sessionId(sessionId)
                .userId(userId)
                .age(age)
                .selfSkinType(selfSkinType)
                .selfConcerns(selfConcerns)
                .allergies(allergies)
                .imageHash(imageHash)
                .consentGiven(consentGiven)
                .consentAt(LocalDateTime.now())
                .consentVersion(consentVersion)
                .status(AnalysisStatus.PROCESSING)
                .build();
        sessionRepository.save(session);
        log.info("Created skin analysis session={} for userId={}", sessionId, userId);

        // 10. Write imageBytes to a temp file so the queue closure holds only a path string (~100 bytes),
        //     not the full image (~10 MB). 50 queued tasks × 10 MB = 500 MB heap otherwise pinned.
        Path tempImagePath;
        try {
            tempImagePath = Files.createTempFile("calmskin-", ".bin");
            Files.write(tempImagePath, imageBytes);
        } catch (Exception e) {
            session.setStatus(AnalysisStatus.FAILED);
            session.setFailureReason("Image could not be buffered for processing");
            sessionRepository.save(session);
            redisTemplate.delete(dedupKey);
            redisTemplate.opsForValue().decrement(limitKey);
            throw new AIServiceUnavailableException("Image buffering failed. Please try again.");
        }

        // 11. Submit path (not bytes) to the bounded executor
        try {
            analysisProcessor.processAsync(sessionId, tempImagePath.toString(), userId);
        } catch (java.util.concurrent.RejectedExecutionException ex) {
            session.setStatus(AnalysisStatus.FAILED);
            session.setFailureReason("Service busy — analysis queue full");
            sessionRepository.save(session);
            try { Files.deleteIfExists(tempImagePath); } catch (Exception ignored) {}
            redisTemplate.delete(dedupKey);
            redisTemplate.opsForValue().decrement(limitKey);
            log.error("Async executor rejected task for sessionId={}", sessionId);
            throw new AIServiceUnavailableException("Too many concurrent analyses. Please try again shortly.");
        }

        return Map.of("sessionId", sessionId);
    }

    @Override
    @Transactional(readOnly = true)
    public SkinAnalysisResultDTO getSession(String sessionId, Long userId, boolean isAdmin) {
        SkinAnalysisSession session = sessionRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new SessionNotFoundException("Session not found: " + sessionId));

        if (!isAdmin && !session.getUserId().equals(userId)) {
            throw new AccessDeniedException("You do not have access to this analysis session");
        }

        return mapToDto(session);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SkinAnalysisResultDTO> getUserHistory(Long userId, Pageable pageable) {
        return sessionRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(this::mapToDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SkinAnalysisResultDTO> getAllSessions(Pageable pageable) {
        return sessionRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(this::mapToDto);
    }

    @Override
    @Transactional
    public void deleteSession(String sessionId, Long userId) {
        SkinAnalysisSession session = sessionRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new SessionNotFoundException("Session not found: " + sessionId));

        if (!session.getUserId().equals(userId)) {
            throw new AccessDeniedException("You do not have permission to delete this session");
        }

        // Delete images from Cloudinary — best-effort, non-blocking
        // Required by Decree 13/2023: user's biometric data must be erasable on demand
        if (session.getOriginalImageUrl() != null) {
            cloudinaryService.delete(sessionId + "-original");
        }
        if (session.getProcessedImageUrl() != null) {
            cloudinaryService.delete(sessionId + "-processed");
        }

        // Evict dedup guard so the same image can be re-analyzed after deletion
        if (session.getImageHash() != null) {
            redisTemplate.delete("ai:skin:dedup:" + session.getImageHash());
        }

        sessionRepository.delete(session);
        log.info("Deleted session={} for userId={}", sessionId, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getAdminStats() {
        long totalSessions = sessionRepository.countAllSessions();
        long completedSessions = sessionRepository.countByStatus(AnalysisStatus.COMPLETED);
        long degradedSessions = sessionRepository.countByStatus(AnalysisStatus.COMPLETED_DEGRADED);
        long failedSessions = sessionRepository.countByStatus(AnalysisStatus.FAILED);
        long processingSessions = sessionRepository.countByStatus(AnalysisStatus.PROCESSING);
        double avgTokens = sessionRepository.avgTokensUsed();
        BigDecimal totalCost = usageLogRepository.sumTotalCostUsd();

        return Map.of(
                "total_sessions", totalSessions,
                "completed", completedSessions,
                "completed_degraded", degradedSessions,
                "failed", failedSessions,
                "processing", processingSessions,
                "avg_tokens", Math.round(avgTokens),
                "total_cost_usd", totalCost != null ? totalCost : BigDecimal.ZERO
        );
    }

    @Override
    @Transactional(readOnly = true)
    public SkinAnalysisResultDTO getLatestForUser(Long userId) {
        SkinAnalysisSession session = sessionRepository.findTopByUserIdOrderByCreatedAtDesc(userId)
                .orElseThrow(() -> new SessionNotFoundException("No analysis session found for user: " + userId));
        return mapToDto(session);
    }

    private SkinAnalysisResultDTO mapToDto(SkinAnalysisSession session) {
        List<String> concerns = parseJsonList(session.getDetectedConcerns());
        List<RecommendedProductDTO> products = parseProductList(session.getRecommendedProductIds());
        RoutineDTO morning = parseRoutine(session.getMorningRoutine());
        RoutineDTO evening = parseRoutine(session.getEveningRoutine());

        return SkinAnalysisResultDTO.builder()
                .sessionId(session.getSessionId())
                .userId(session.getUserId())
                .status(session.getStatus())
                .detectedSkinType(session.getDetectedSkinType())
                .detectedConcerns(concerns)
                .skinConditionReport(session.getSkinConditionReport())
                .recommendedProducts(products)
                .morningRoutine(morning)
                .eveningRoutine(evening)
                .failureReason(session.getFailureReason())
                .createdAt(session.getCreatedAt())
                .completedAt(session.getCompletedAt())
                .build();
    }

    private List<String> parseJsonList(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            log.debug("Could not parse JSON list: {}", e.getMessage());
            return List.of();
        }
    }

    private List<RecommendedProductDTO> parseProductList(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json, new TypeReference<List<RecommendedProductDTO>>() {});
        } catch (Exception e) {
            log.debug("Could not parse product list: {}", e.getMessage());
            return List.of();
        }
    }

    private RoutineDTO parseRoutine(String json) {
        if (json == null || json.isBlank()) return RoutineDTO.builder().steps(List.of()).build();
        try {
            return objectMapper.readValue(json, RoutineDTO.class);
        } catch (Exception e) {
            log.debug("Could not parse routine: {}", e.getMessage());
            return RoutineDTO.builder().steps(List.of()).build();
        }
    }

    private void validateMagicBytes(byte[] data, String declaredContentType) {
        if (data == null || data.length < 12) {
            throw new InvalidImageFormatException("Image data is too short to be a valid image file");
        }
        boolean valid = startsWith(data, MAGIC_JPEG)
                || startsWith(data, MAGIC_PNG)
                || (startsWith(data, MAGIC_RIFF) && matchesAt(data, 8, MAGIC_WEBP));
        if (!valid) {
            throw new InvalidImageFormatException(
                    "Image file signature does not match declared type '" + declaredContentType
                    + "'. Upload a real JPEG, PNG, or WebP file.");
        }
    }

    private boolean startsWith(byte[] data, byte[] prefix) {
        if (data.length < prefix.length) return false;
        for (int i = 0; i < prefix.length; i++) {
            if (data[i] != prefix[i]) return false;
        }
        return true;
    }

    private boolean matchesAt(byte[] data, int offset, byte[] pattern) {
        if (data.length < offset + pattern.length) return false;
        for (int i = 0; i < pattern.length; i++) {
            if (data[offset + i] != pattern[i]) return false;
        }
        return true;
    }

    private String computeSha256(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data);
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            log.error("SHA-256 computation failed: {}", e.getMessage());
            return UUID.randomUUID().toString().replace("-", "");
        }
    }
}
