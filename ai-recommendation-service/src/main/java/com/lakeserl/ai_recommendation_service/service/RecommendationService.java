package com.lakeserl.ai_recommendation_service.service;

import com.lakeserl.ai_recommendation_service.dto.RecommendationResponseDTO;

import java.util.UUID;

public interface RecommendationService {

    RecommendationResponseDTO forMe(UUID userId);

    RecommendationResponseDTO similar(Long productId);

    RecommendationResponseDTO trending(int limit);

    RecommendationResponseDTO frequentlyBoughtWith(Long productId);
}
