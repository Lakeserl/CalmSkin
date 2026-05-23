package com.lakeserl.review_service.repository;

import com.lakeserl.review_service.entity.ReviewReply;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReviewReplyRepository extends JpaRepository<ReviewReply, Long> {
    List<ReviewReply> findByReviewIdOrderByCreatedAtAsc(Long reviewId);
    Optional<ReviewReply> findByIdAndUserId(Long id, UUID userId);
}

