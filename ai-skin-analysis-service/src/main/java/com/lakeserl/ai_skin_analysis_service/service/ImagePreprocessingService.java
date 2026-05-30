package com.lakeserl.ai_skin_analysis_service.service;

public interface ImagePreprocessingService {

    boolean hasFace(byte[] imageBytes);

    byte[] normalize(byte[] imageBytes);

    String extractFeatures(byte[] imageBytes);
}
