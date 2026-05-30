package com.lakeserl.ai_skin_analysis_service.service;

import com.lakeserl.ai_skin_analysis_service.opencv.FaceDetector;
import com.lakeserl.ai_skin_analysis_service.opencv.FeatureExtractor;
import com.lakeserl.ai_skin_analysis_service.opencv.ImageNormalizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImagePreprocessingServiceImpl implements ImagePreprocessingService {

    private final FaceDetector faceDetector;
    private final ImageNormalizer imageNormalizer;
    private final FeatureExtractor featureExtractor;

    @Override
    public boolean hasFace(byte[] imageBytes) {
        return faceDetector.hasFace(imageBytes);
    }

    @Override
    public byte[] normalize(byte[] imageBytes) {
        return imageNormalizer.normalize(imageBytes);
    }

    @Override
    public String extractFeatures(byte[] imageBytes) {
        return featureExtractor.extractFeatures(imageBytes);
    }
}
