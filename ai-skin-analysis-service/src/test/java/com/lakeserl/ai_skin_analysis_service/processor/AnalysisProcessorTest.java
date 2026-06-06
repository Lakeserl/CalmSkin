package com.lakeserl.ai_skin_analysis_service.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lakeserl.ai_skin_analysis_service.ai.SkinAnalysisAiResult;
import com.lakeserl.ai_skin_analysis_service.ai.SkinAnalysisNormalizer;
import com.lakeserl.ai_skin_analysis_service.client.ProductServiceClient;
import com.lakeserl.ai_skin_analysis_service.entity.SkinAnalysisSession;
import com.lakeserl.ai_skin_analysis_service.enums.AnalysisStatus;
import com.lakeserl.ai_skin_analysis_service.repository.OutboxRepository;
import com.lakeserl.ai_skin_analysis_service.repository.AIUsageLogRepository;
import com.lakeserl.ai_skin_analysis_service.repository.SkinAnalysisSessionRepository;
import com.lakeserl.ai_skin_analysis_service.service.AIAnalysisService;
import com.lakeserl.ai_skin_analysis_service.service.CloudinaryService;
import com.lakeserl.ai_skin_analysis_service.service.ImagePreprocessingService;
import com.lakeserl.ai_skin_analysis_service.service.RoutineGeneratorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The session's terminal status is the persisted source of truth for the 3-state model:
 *   COMPLETED (genuine), COMPLETED_DEGRADED (AI failed -> fallback), FAILED (no result).
 * A degraded result must NOT publish the profile-update event (KI-1: don't propagate a
 * fabricated NORMAL). A genuine result must normalize (KI-1) and publish.
 */
class AnalysisProcessorTest {

    private SkinAnalysisSessionRepository sessionRepository;
    private AIAnalysisService aiAnalysisService;
    private OutboxRepository outboxRepository;
    private AnalysisProcessor processor;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        sessionRepository = mock(SkinAnalysisSessionRepository.class);
        AIUsageLogRepository usageLogRepository = mock(AIUsageLogRepository.class);
        ImagePreprocessingService preprocessingService = mock(ImagePreprocessingService.class);
        aiAnalysisService = mock(AIAnalysisService.class);
        CloudinaryService cloudinaryService = mock(CloudinaryService.class);
        RoutineGeneratorService routineGeneratorService = mock(RoutineGeneratorService.class);
        ProductServiceClient productServiceClient = mock(ProductServiceClient.class);
        outboxRepository = mock(OutboxRepository.class);
        RedisTemplate<String, String> redisTemplate = mock(RedisTemplate.class);

        processor = new AnalysisProcessor(
                sessionRepository, usageLogRepository, preprocessingService, aiAnalysisService,
                cloudinaryService, routineGeneratorService, productServiceClient, outboxRepository,
                redisTemplate, new ObjectMapper(), new SkinAnalysisNormalizer());

        when(preprocessingService.hasFace(any())).thenReturn(true);
        when(preprocessingService.normalize(any())).thenReturn(new byte[]{1, 2, 3});
        when(preprocessingService.extractFeatures(any())).thenReturn("features");
        when(cloudinaryService.uploadAsync(any(), any())).thenReturn(CompletableFuture.<String>completedFuture(null));
        when(productServiceClient.findBySkinProfile(any(), any())).thenReturn(List.of());
    }

    @Test
    void degradedAiResultRecordsCompletedDegradedAndSkipsKafka() throws Exception {
        SkinAnalysisSession session = stubSession("s-degraded");
        when(aiAnalysisService.analyze(any(), any(), any(), any(), any()))
                .thenReturn(SkinAnalysisAiResult.degradedFallback(10L));

        processor.processAsync("s-degraded", writeTempImage().toString(), 1L);

        assertThat(session.getStatus()).isEqualTo(AnalysisStatus.COMPLETED_DEGRADED);
        verify(outboxRepository, never()).save(any());
    }

    @Test
    void genuineResultRecordsCompletedNormalizesAndPublishesKafka() throws Exception {
        SkinAnalysisSession session = stubSession("s-ok");
        SkinAnalysisAiResult aiResult = SkinAnalysisAiResult.builder()
                .detectedSkinType("da dầu")        // Vietnamese -> must persist as OILY (KI-1)
                .concerns(List.of("mụn"))          // -> ACNE
                .morningSteps(List.of("CLEANSE"))
                .eveningSteps(List.of("CLEANSE"))
                .advice("real advice")
                .tokensInput(10).tokensOutput(20).responseTimeMs(100L)
                .build();
        when(aiAnalysisService.analyze(any(), any(), any(), any(), any())).thenReturn(aiResult);

        processor.processAsync("s-ok", writeTempImage().toString(), 1L);

        assertThat(session.getStatus()).isEqualTo(AnalysisStatus.COMPLETED);
        assertThat(session.getDetectedSkinType()).isEqualTo("OILY");
        verify(outboxRepository).save(any());
    }

    private SkinAnalysisSession stubSession(String sessionId) {
        SkinAnalysisSession session = SkinAnalysisSession.builder()
                .sessionId(sessionId).userId(1L).status(AnalysisStatus.PROCESSING).build();
        when(sessionRepository.findBySessionId(sessionId)).thenReturn(Optional.of(session));
        return session;
    }

    private Path writeTempImage() throws Exception {
        Path p = Files.createTempFile("calmskin-test-", ".bin");
        Files.write(p, new byte[]{9, 9, 9, 9});
        return p;
    }
}
