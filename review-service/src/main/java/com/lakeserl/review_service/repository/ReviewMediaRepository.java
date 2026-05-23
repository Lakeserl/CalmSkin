package com.lakeserl.review_service.repository;

import com.lakeserl.review_service.entity.ReviewMedia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReviewMediaRepository extends JpaRepository<ReviewMedia, Long> {
    List<ReviewMedia> findByReviewIdOrderBySortOrderAsc(Long reviewId);
    Optional<ReviewMedia> findByIdAndReviewId(Long id, Long reviewId);
    int countByReviewId(Long reviewId);
}
