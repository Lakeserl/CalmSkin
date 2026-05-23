package com.lakeserl.review_service.controller;

import com.lakeserl.review_service.dto.request.AdminUpdateReportRequest;
import com.lakeserl.review_service.dto.request.AdminUpdateReviewRequest;
import com.lakeserl.review_service.dto.response.*;
import com.lakeserl.review_service.enums.ReportStatus;
import com.lakeserl.review_service.enums.ReviewStatus;
import com.lakeserl.review_service.service.ReviewReplyService;
import com.lakeserl.review_service.service.ReviewReportService;
import com.lakeserl.review_service.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/reviews")
@RequiredArgsConstructor
@Tag(name = "Admin Reviews", description = "Review moderation and management (ADMIN only)")
public class AdminReviewController {

    private final ReviewService reviewService;
    private final ReviewReportService reportService;
    private final ReviewReplyService replyService;

    @GetMapping
    @Operation(summary = "List all reviews with optional status/product filter")
    public ApiResponse<Page<ReviewDTO>> listReviews(
            @RequestParam(required = false) ReviewStatus status,
            @RequestParam(required = false) Long productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(reviewService.adminListReviews(status, productId,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))));
    }

    @PatchMapping("/{reviewId}/status")
    @Operation(summary = "Update review status (publish, hide, delete)")
    public ApiResponse<ReviewDTO> updateStatus(
            @PathVariable Long reviewId,
            @RequestHeader("X-User-Id") UUID adminId,
            @Valid @RequestBody AdminUpdateReviewRequest request) {
        return ApiResponse.ok("Review updated", reviewService.adminUpdateReview(reviewId, adminId, request));
    }

    @GetMapping("/stats")
    @Operation(summary = "Admin review dashboard stats")
    public ApiResponse<AdminReviewStatsDTO> getStats() {
        return ApiResponse.ok(reviewService.adminGetStats());
    }

    // ── Reports moderation ────────────────────────────────────────────────────

    @GetMapping("/reports")
    @Operation(summary = "List review reports")
    public ApiResponse<Page<ReviewReportDTO>> listReports(
            @RequestParam(defaultValue = "PENDING") ReportStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(reportService.adminListReports(status,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))));
    }

    @PatchMapping("/reports/{reportId}/status")
    @Operation(summary = "Dismiss or act on a report")
    public ApiResponse<ReviewReportDTO> resolveReport(
            @PathVariable Long reportId,
            @RequestHeader("X-User-Id") UUID adminId,
            @Valid @RequestBody AdminUpdateReportRequest request) {
        return ApiResponse.ok("Report updated", reportService.adminUpdateReport(reportId, adminId, request));
    }

    // ── Reply management ──────────────────────────────────────────────────────

    @DeleteMapping("/replies/{replyId}")
    @Operation(summary = "Delete any reply (admin)")
    public ApiResponse<Void> deleteReply(@PathVariable Long replyId) {
        replyService.adminDeleteReply(replyId);
        return ApiResponse.ok(null);
    }
}

