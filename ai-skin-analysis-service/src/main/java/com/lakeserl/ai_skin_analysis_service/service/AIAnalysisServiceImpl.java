package com.lakeserl.ai_skin_analysis_service.service;

import com.lakeserl.ai_skin_analysis_service.ai.GeminiClient;
import com.lakeserl.ai_skin_analysis_service.ai.SkinAnalysisAiResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AIAnalysisServiceImpl implements AIAnalysisService {

    private final GeminiClient geminiClient;

    @Override
    public SkinAnalysisAiResult analyze(byte[] normalizedImageBytes, String features,
                                         Integer age, String selfSkinType, String concerns) {
        try {
            log.info("Calling Gemini for skin analysis, imageSize={} bytes", normalizedImageBytes.length);
            return geminiClient.analyzeImage(normalizedImageBytes, features, age, selfSkinType, concerns);
        } catch (Exception e) {
            // Never fail the whole session on an AI error. The processor records it as
            // COMPLETED_DEGRADED via the degraded flag and the user still gets a basic result.
            log.error("AI analysis failed: {} — returning degraded fallback", e.getMessage(), e);
            return SkinAnalysisAiResult.degradedFallback(0L);
        }
    }
}
