package com.lakeserl.review_service.controller;

import com.lakeserl.review_service.dto.request.CreateReportRequest;
import com.lakeserl.review_service.dto.response.ApiResponse;
import com.lakeserl.review_service.dto.response.ReviewReportDTO;
import com.lakeserl.review_service.service.ReviewReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reviews/{reviewId}/reports")
@RequiredArgsConstructor
@Tag(name = "Review Reports", description = "Report inappropriate reviews")
public class ReviewReportController {

    private final ReviewReportService reportService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Report a review")
    public ApiResponse<ReviewReportDTO> createReport(
            @PathVariable Long reviewId,
            @RequestHeader("X-User-Id") UUID userId,
            @Valid @RequestBody CreateReportRequest request) {
        return ApiResponse.ok("Report submitted", reportService.createReport(reviewId, userId, request));
    }
}

