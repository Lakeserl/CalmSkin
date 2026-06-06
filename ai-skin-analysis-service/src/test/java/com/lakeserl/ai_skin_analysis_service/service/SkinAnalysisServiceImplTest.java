package com.lakeserl.ai_skin_analysis_service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lakeserl.ai_skin_analysis_service.exception.BiometricConsentMissingException;
import com.lakeserl.ai_skin_analysis_service.exception.DailyLimitExceededException;
import com.lakeserl.ai_skin_analysis_service.exception.ImageTooLargeException;
import com.lakeserl.ai_skin_analysis_service.exception.InvalidImageFormatException;
import com.lakeserl.ai_skin_analysis_service.processor.AnalysisProcessor;
import com.lakeserl.ai_skin_analysis_service.repository.AIUsageLogRepository;
import com.lakeserl.ai_skin_analysis_service.repository.SkinAnalysisSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Input validation (B.1) + daily rate-limit (D.1) at the API entry — these run BEFORE any
 * Gemini cost is incurred, so they must reject bad/over-limit requests deterministically.
 */
class SkinAnalysisServiceImplTest {

    private SkinAnalysisSessionRepository sessionRepository;
    private ValueOperations<String, String> valueOps;
    private SkinAnalysisServiceImpl service;

    // Valid PNG header (8 magic bytes) padded past the 12-byte minimum the validator requires.
    private static final byte[] PNG = {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52
    };

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        sessionRepository = mock(SkinAnalysisSessionRepository.class);
        AIUsageLogRepository usageLogRepository = mock(AIUsageLogRepository.class);
        AnalysisProcessor analysisProcessor = mock(AnalysisProcessor.class);
        CloudinaryService cloudinaryService = mock(CloudinaryService.class);
        RedisTemplate<String, String> redisTemplate = mock(RedisTemplate.class);
        valueOps = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);

        service = new SkinAnalysisServiceImpl(
                sessionRepository, usageLogRepository, analysisProcessor,
                cloudinaryService, redisTemplate, new ObjectMapper());
        ReflectionTestUtils.setField(service, "dailyLimit", 10);
        ReflectionTestUtils.setField(service, "consentVersion", "1.0");
    }

    @Test
    void rejectsWhenConsentMissing() {
        MultipartFile img = new MockMultipartFile("image", "f.png", "image/png", PNG);
        assertThatThrownBy(() -> service.initiateAnalysis(img, 25, "OILY", "ACNE", null, false, 1L))
                .isInstanceOf(BiometricConsentMissingException.class);
    }

    @Test
    void rejectsUnsupportedMimeType() {
        MultipartFile img = new MockMultipartFile("image", "f.txt", "text/plain", PNG);
        assertThatThrownBy(() -> service.initiateAnalysis(img, 25, "OILY", "ACNE", null, true, 1L))
                .isInstanceOf(InvalidImageFormatException.class);
    }

    @Test
    void rejectsOversizeImage() {
        MultipartFile img = mock(MultipartFile.class);
        when(img.getContentType()).thenReturn("image/png");
        when(img.getSize()).thenReturn(11L * 1024 * 1024);
        assertThatThrownBy(() -> service.initiateAnalysis(img, 25, "OILY", "ACNE", null, true, 1L))
                .isInstanceOf(ImageTooLargeException.class);
    }

    @Test
    void rejectsContentNotMatchingMagicBytes() {
        byte[] fake = "this-is-definitely-not-an-image".getBytes();
        MultipartFile img = new MockMultipartFile("image", "f.png", "image/png", fake);
        assertThatThrownBy(() -> service.initiateAnalysis(img, 25, "OILY", "ACNE", null, true, 1L))
                .isInstanceOf(InvalidImageFormatException.class);
    }

    @Test
    void rejectsWhenDailyLimitExceeded() {
        MultipartFile img = new MockMultipartFile("image", "f.png", "image/png", PNG);
        when(valueOps.get(anyString())).thenReturn(null);       // no dedup hit
        when(valueOps.increment(anyString())).thenReturn(11L);  // over the limit of 10
        assertThatThrownBy(() -> service.initiateAnalysis(img, 25, "OILY", "ACNE", null, true, 1L))
                .isInstanceOf(DailyLimitExceededException.class);
    }

    @Test
    void acceptsValidImageUnderLimitAndReturnsSessionId() {
        MultipartFile img = new MockMultipartFile("image", "f.png", "image/png", PNG);
        when(valueOps.get(anyString())).thenReturn(null);
        when(valueOps.increment(anyString())).thenReturn(1L);
        when(valueOps.setIfAbsent(anyString(), anyString(), anyLong(), any())).thenReturn(true);

        Map<String, String> result = service.initiateAnalysis(img, 25, "OILY", "ACNE", null, true, 1L);

        assertThat(result).containsKey("sessionId");
    }
}
