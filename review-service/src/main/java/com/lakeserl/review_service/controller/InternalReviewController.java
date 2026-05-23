package com.lakeserl.review_service.controller;

import com.lakeserl.review_service.dto.response.ApiResponse;
import com.lakeserl.review_service.dto.response.ReviewSummaryDTO;
import com.lakeserl.review_service.service.ReviewSummaryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * Internal endpoints consumed by other microservices (product-service, etc.)
 * via X-Internal-Secret header. Not exposed publicly via gateway.
 */
@RestController
@RequestMapping("/internal/reviews")
@RequiredArgsConstructor
@Tag(name = "Internal Reviews", description = "Internal service-to-service review APIs")
public class InternalReviewController {

    private final ReviewSummaryService summaryService;

    @GetMapping("/products/{productId}/summary")
    @Operation(summary = "Get aggregated review summary (internal)")
    public ApiResponse<ReviewSummaryDTO> getSummary(@PathVariable Long productId) {
        return ApiResponse.ok(summaryService.getSummary(productId));
    }
}
