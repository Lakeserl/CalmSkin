package com.lakeserl.review_service.controller;

import com.lakeserl.review_service.dto.request.PresignRequest;
import com.lakeserl.review_service.dto.response.ApiResponse;
import com.lakeserl.review_service.dto.response.PresignResponse;
import com.lakeserl.review_service.service.MediaPresignService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reviews/media")
@RequiredArgsConstructor
@Tag(name = "Review Media", description = "S3 pre-signed URL generation for review photos/videos")
public class ReviewMediaController {

    private final MediaPresignService presignService;

    @PostMapping("/presign")
    @Operation(summary = "Get a pre-signed S3 upload URL for a review media file")
    public ApiResponse<PresignResponse> presign(
            @RequestHeader("X-User-Id") UUID userId,
            @Valid @RequestBody PresignRequest request) {
        return ApiResponse.ok(presignService.presign(userId, request));
    }
}

