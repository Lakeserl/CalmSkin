package com.lakeserl.ai_skin_analysis_service.service;

import com.lakeserl.ai_skin_analysis_service.ai.SkinAnalysisAiResult;

public interface AIAnalysisService {

    SkinAnalysisAiResult analyze(byte[] normalizedImageBytes, String features,
                                  Integer age, String selfSkinType, String concerns);
}
