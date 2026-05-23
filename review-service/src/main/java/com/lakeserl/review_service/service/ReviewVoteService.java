package com.lakeserl.review_service.service;

import com.lakeserl.review_service.dto.request.VoteRequest;
import com.lakeserl.review_service.entity.Review;
import com.lakeserl.review_service.entity.ReviewVote;
import com.lakeserl.review_service.exception.DuplicateVoteException;
import com.lakeserl.review_service.exception.ReviewNotFoundException;
import com.lakeserl.review_service.repository.ReviewRepository;
import com.lakeserl.review_service.repository.ReviewVoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReviewVoteService {

    private final ReviewRepository reviewRepository;
    private final ReviewVoteRepository voteRepository;

    @Transactional
    public void vote(Long reviewId, UUID userId, VoteRequest request) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewNotFoundException("Review " + reviewId + " not found."));

        // Allow re-vote: update existing vote
        voteRepository.findByReviewIdAndUserId(reviewId, userId).ifPresentOrElse(
            existing -> {
                boolean sameVote = existing.isHelpful() == request.helpful();
                if (sameVote) throw new DuplicateVoteException("You have already cast this vote.");
                // Flip vote
                if (existing.isHelpful()) {
                    review.setHelpfulCount(Math.max(0, review.getHelpfulCount() - 1));
                    review.setNotHelpfulCount(review.getNotHelpfulCount() + 1);
                } else {
                    review.setNotHelpfulCount(Math.max(0, review.getNotHelpfulCount() - 1));
                    review.setHelpfulCount(review.getHelpfulCount() + 1);
                }
                existing.setHelpful(request.helpful());
                voteRepository.save(existing);
                reviewRepository.save(review);
            },
            () -> {
                ReviewVote vote = ReviewVote.builder()
                        .review(review)
                        .userId(userId)
                        .helpful(request.helpful())
                        .build();
                voteRepository.save(vote);
                if (request.helpful()) {
                    review.setHelpfulCount(review.getHelpfulCount() + 1);
                } else {
                    review.setNotHelpfulCount(review.getNotHelpfulCount() + 1);
                }
                reviewRepository.save(review);
            }
        );
    }
}

