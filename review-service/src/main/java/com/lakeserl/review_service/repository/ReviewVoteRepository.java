package com.lakeserl.review_service.repository;

import com.lakeserl.review_service.entity.ReviewVote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ReviewVoteRepository extends JpaRepository<ReviewVote, Long> {
    Optional<ReviewVote> findByReviewIdAndUserId(Long reviewId, UUID userId);
    boolean existsByReviewIdAndUserId(Long reviewId, UUID userId);
}

