package com.lakeserl.review_service.controller;

import com.lakeserl.review_service.dto.request.CreateReviewRequest;
import com.lakeserl.review_service.dto.request.UpdateReviewRequest;
import com.lakeserl.review_service.dto.response.ApiResponse;
import com.lakeserl.review_service.dto.response.EligibilityDTO;
import com.lakeserl.review_service.dto.response.ReviewDTO;
import com.lakeserl.review_service.dto.response.ReviewSummaryDTO;
import com.lakeserl.review_service.service.ReviewEligibilityService;
import com.lakeserl.review_service.service.ReviewService;
import com.lakeserl.review_service.service.ReviewSummaryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reviews")
@RequiredArgsConstructor
@Tag(name = "Reviews", description = "Product review browsing and management")
public class ReviewController {

    private final ReviewService reviewService;
    private final ReviewSummaryService summaryService;
    private final ReviewEligibilityService eligibilityService;

    // ── Public / customer endpoints ──────────────────────────────────────────

    @GetMapping("/products/{productId}")
    @Operation(summary = "Get reviews for a product")
    public ApiResponse<Page<ReviewDTO>> getProductReviews(
            @PathVariable Long productId,
            @RequestParam(required = false) Short rating,
            @RequestParam(required = false) String skinType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "desc") String dir) {
        Sort.Direction direction = "asc".equalsIgnoreCase(dir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        var pageable = PageRequest.of(page, size, Sort.by(direction, sort));
        return ApiResponse.ok(reviewService.getProductReviews(productId, rating, skinType, pageable));
    }

    @GetMapping("/products/{productId}/summary")
    @Operation(summary = "Get aggregated rating summary for a product")
    public ApiResponse<ReviewSummaryDTO> getProductSummary(@PathVariable Long productId) {
        return ApiResponse.ok(summaryService.getSummary(productId));
    }

    @GetMapping("/{reviewId}")
    @Operation(summary = "Get a single review by ID")
    public ApiResponse<ReviewDTO> getReview(
            @PathVariable Long reviewId,
            @RequestHeader(value = "X-User-Id", required = false) UUID currentUserId) {
        return ApiResponse.ok(reviewService.getReview(reviewId, currentUserId));
    }

    // ── Authenticated user endpoints ─────────────────────────────────────────

    @PostMapping("/products/{productId}")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a review for a product (verified purchase required)")
    public ApiResponse<ReviewDTO> createReview(
            @PathVariable Long productId,
            @RequestHeader("X-User-Id") UUID userId,
            @Valid @RequestBody CreateReviewRequest request) {
        return ApiResponse.ok("Review created", reviewService.createReview(userId, productId, request));
    }

    @PutMapping("/{reviewId}")
    @Operation(summary = "Update own review (within 30-day edit window)")
    public ApiResponse<ReviewDTO> updateReview(
            @PathVariable Long reviewId,
            @RequestHeader("X-User-Id") UUID userId,
            @Valid @RequestBody UpdateReviewRequest request) {
        return ApiResponse.ok("Review updated", reviewService.updateReview(reviewId, userId, request));
    }

    @DeleteMapping("/{reviewId}")
    @Operation(summary = "Soft-delete own review")
    public ApiResponse<Void> deleteReview(
            @PathVariable Long reviewId,
            @RequestHeader("X-User-Id") UUID userId) {
        reviewService.deleteReview(reviewId, userId);
        return ApiResponse.ok(null);
    }

    @GetMapping("/me")
    @Operation(summary = "List own reviews")
    public ApiResponse<Page<ReviewDTO>> getMyReviews(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(reviewService.getMyReviews(userId,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))));
    }

    @GetMapping("/me/eligible")
    @Operation(summary = "Get order items the user is eligible to review")
    public ApiResponse<List<EligibilityDTO>> getEligibleItems(
            @RequestHeader("X-User-Id") UUID userId) {
        return ApiResponse.ok(eligibilityService.getEligibleItems(userId));
    }
}

