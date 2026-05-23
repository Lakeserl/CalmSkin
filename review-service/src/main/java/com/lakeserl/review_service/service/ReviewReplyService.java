package com.lakeserl.review_service.service;

import com.lakeserl.review_service.dto.request.CreateReplyRequest;
import com.lakeserl.review_service.dto.request.UpdateReplyRequest;
import com.lakeserl.review_service.dto.response.ReviewReplyDTO;
import com.lakeserl.review_service.entity.Review;
import com.lakeserl.review_service.entity.ReviewReply;
import com.lakeserl.review_service.exception.ForbiddenException;
import com.lakeserl.review_service.exception.ReviewNotFoundException;
import com.lakeserl.review_service.repository.ReviewReplyRepository;
import com.lakeserl.review_service.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReviewReplyService {

    private final ReviewRepository reviewRepository;
    private final ReviewReplyRepository replyRepository;

    @Transactional(readOnly = true)
    public List<ReviewReplyDTO> getReplies(Long reviewId) {
        return replyRepository.findByReviewIdOrderByCreatedAtAsc(reviewId)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional
    public ReviewReplyDTO createReply(Long reviewId, UUID userId, boolean isSeller, CreateReplyRequest request) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewNotFoundException("Review " + reviewId + " not found."));
        ReviewReply reply = ReviewReply.builder()
                .review(review)
                .userId(userId)
                .seller(isSeller)
                .body(request.body())
                .build();
        replyRepository.save(reply);
        return toDTO(reply);
    }

    @Transactional
    public ReviewReplyDTO updateReply(Long replyId, UUID userId, UpdateReplyRequest request) {
        ReviewReply reply = replyRepository.findByIdAndUserId(replyId, userId)
                .orElseThrow(() -> new ReviewNotFoundException("Reply not found or does not belong to you."));
        reply.setBody(request.body());
        replyRepository.save(reply);
        return toDTO(reply);
    }

    @Transactional
    public void deleteReply(Long replyId, UUID userId) {
        ReviewReply reply = replyRepository.findByIdAndUserId(replyId, userId)
                .orElseThrow(() -> new ReviewNotFoundException("Reply not found or does not belong to you."));
        replyRepository.delete(reply);
    }

    @Transactional
    public void adminDeleteReply(Long replyId) {
        replyRepository.findById(replyId)
                .orElseThrow(() -> new ReviewNotFoundException("Reply " + replyId + " not found."));
        replyRepository.deleteById(replyId);
    }

    private ReviewReplyDTO toDTO(ReviewReply reply) {
        return ReviewReplyDTO.builder()
                .id(reply.getId())
                .reviewId(reply.getReview().getId())
                .userId(reply.getUserId())
                .seller(reply.isSeller())
                .body(reply.getBody())
                .createdAt(reply.getCreatedAt())
                .updatedAt(reply.getUpdatedAt())
                .build();
    }
}

