package com.lakeserl.ai_recommendation_service.controller;

import com.lakeserl.ai_recommendation_service.dto.ApiResponse;
import com.lakeserl.ai_recommendation_service.dto.RecommendationResponseDTO;
import com.lakeserl.ai_recommendation_service.service.RecommendationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/recommendations")
@RequiredArgsConstructor
@Tag(name = "Recommendations", description = "Personalized product recommendations")
public class RecommendationController {

    private final RecommendationService recommendationService;

    @GetMapping("/for-me")
    @Operation(summary = "Personalized recommendations based on user's skin profile")
    public ResponseEntity<ApiResponse<RecommendationResponseDTO>> forMe(
            @RequestHeader("X-User-Id") String userId) {

        RecommendationResponseDTO result = recommendationService.forMe(UUID.fromString(userId));
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("/similar/{productId}")
    @Operation(summary = "Products similar to a given product (no auth required)")
    public ResponseEntity<ApiResponse<RecommendationResponseDTO>> similar(
            @PathVariable Long productId) {

        return ResponseEntity.ok(ApiResponse.ok(recommendationService.similar(productId)));
    }

    @GetMapping("/frequently-bought-with/{productId}")
    @Operation(summary = "Frequently bought together with a given product")
    public ResponseEntity<ApiResponse<RecommendationResponseDTO>> frequentlyBoughtWith(
            @PathVariable Long productId) {

        return ResponseEntity.ok(ApiResponse.ok(recommendationService.frequentlyBoughtWith(productId)));
    }

    @GetMapping("/trending")
    @Operation(summary = "Trending products based on order history")
    public ResponseEntity<ApiResponse<RecommendationResponseDTO>> trending(
            @RequestParam(defaultValue = "10") int limit) {

        return ResponseEntity.ok(ApiResponse.ok(recommendationService.trending(Math.min(limit, 20))));
    }
}
