package com.lakeserl.review_service.controller;

import com.lakeserl.review_service.dto.request.VoteRequest;
import com.lakeserl.review_service.dto.response.ApiResponse;
import com.lakeserl.review_service.service.ReviewVoteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reviews/{reviewId}/votes")
@RequiredArgsConstructor
@Tag(name = "Review Votes", description = "Helpful / not-helpful votes on reviews")
public class ReviewVoteController {

    private final ReviewVoteService voteService;

    @PostMapping
    @Operation(summary = "Vote on a review (helpful/not-helpful)")
    public ApiResponse<Void> vote(
            @PathVariable Long reviewId,
            @RequestHeader("X-User-Id") UUID userId,
            @Valid @RequestBody VoteRequest request) {
        voteService.vote(reviewId, userId, request);
        return ApiResponse.ok(null);
    }
}

