package com.lakeserl.product_service.controller;

import com.lakeserl.product_service.dto.response.ApiResponse;
import com.lakeserl.product_service.dto.response.ProductSummaryDTO;
import com.lakeserl.product_service.service.RecommendationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Personalized product recommendations based on:
 * - Skin profile (skinType + skinConcerns from user-service)
 * - Purchase history exclusion (already-purchased products are excluded)
 * - Brand affinity boost (brands with prior purchases rank higher)
 *
 * Authentication: expects X-User-Id header from API gateway.
 */
@RestController
@RequestMapping("/api/v1/recommendations")
@RequiredArgsConstructor
@Tag(name = "Recommendations", description = "Personalized product recommendations")
public class RecommendationController {

    private final RecommendationService recommendationService;

    @GetMapping("/me")
    @Operation(
        summary = "Get personalized product recommendations",
        description = "Returns products matching the authenticated user's skin profile. " +
                      "Excludes already-purchased products and boosts preferred brands. " +
                      "Falls back to best sellers if no skin profile is set."
    )
    public ApiResponse<List<ProductSummaryDTO>> getRecommendations(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestParam(defaultValue = "10") int limit) {
        return ApiResponse.ok(recommendationService.getRecommendations(userId, limit));
    }
}
