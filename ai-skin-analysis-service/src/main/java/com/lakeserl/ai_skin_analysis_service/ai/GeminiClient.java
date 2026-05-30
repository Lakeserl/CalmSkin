package com.lakeserl.ai_skin_analysis_service.ai;

public interface GeminiClient {

    SkinAnalysisAiResult analyzeImage(byte[] imageBytes, String features,
                                       Integer age, String selfSkinType,
                                       String concerns);
}
