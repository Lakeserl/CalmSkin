package com.lakeserl.ai_recommendation_service.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class RecommendationResponseDTO {
    private String strategy;   // SKIN_PROFILE, SIMILAR, TRENDING, FREQUENTLY_BOUGHT_WITH
    private List<ProductDTO> products;
}
