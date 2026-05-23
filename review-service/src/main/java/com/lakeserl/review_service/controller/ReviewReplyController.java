package com.lakeserl.review_service.controller;

import com.lakeserl.review_service.dto.request.CreateReplyRequest;
import com.lakeserl.review_service.dto.request.UpdateReplyRequest;
import com.lakeserl.review_service.dto.response.ApiResponse;
import com.lakeserl.review_service.dto.response.ReviewReplyDTO;
import com.lakeserl.review_service.service.ReviewReplyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reviews/{reviewId}/replies")
@RequiredArgsConstructor
@Tag(name = "Review Replies", description = "Reply to reviews (customers and sellers)")
public class ReviewReplyController {

    private final ReviewReplyService replyService;

    @GetMapping
    @Operation(summary = "List replies for a review")
    public ApiResponse<List<ReviewReplyDTO>> getReplies(@PathVariable Long reviewId) {
        return ApiResponse.ok(replyService.getReplies(reviewId));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add a reply to a review")
    public ApiResponse<ReviewReplyDTO> createReply(
            @PathVariable Long reviewId,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader(value = "X-User-Role", defaultValue = "USER") String role,
            @Valid @RequestBody CreateReplyRequest request) {
        boolean isSeller = role.contains("SELLER") || role.contains("ADMIN");
        return ApiResponse.ok("Reply created", replyService.createReply(reviewId, userId, isSeller, request));
    }

    @PutMapping("/{replyId}")
    @Operation(summary = "Update own reply")
    public ApiResponse<ReviewReplyDTO> updateReply(
            @PathVariable Long reviewId,
            @PathVariable Long replyId,
            @RequestHeader("X-User-Id") UUID userId,
            @Valid @RequestBody UpdateReplyRequest request) {
        return ApiResponse.ok("Reply updated", replyService.updateReply(replyId, userId, request));
    }

    @DeleteMapping("/{replyId}")
    @Operation(summary = "Delete own reply")
    public ApiResponse<Void> deleteReply(
            @PathVariable Long reviewId,
            @PathVariable Long replyId,
            @RequestHeader("X-User-Id") UUID userId) {
        replyService.deleteReply(replyId, userId);
        return ApiResponse.ok(null);
    }
}

