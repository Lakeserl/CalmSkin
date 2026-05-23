package com.lakeserl.review_service.repository;

import com.lakeserl.review_service.entity.ReviewEligibility;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReviewEligibilityRepository extends JpaRepository<ReviewEligibility, Long> {

    Optional<ReviewEligibility> findByUserIdAndOrderItemId(UUID userId, Long orderItemId);

    @Query("SELECT e FROM ReviewEligibility e WHERE e.userId = :userId AND e.reviewId IS NULL")
    List<ReviewEligibility> findEligibleByUserId(@Param("userId") UUID userId);

    List<ReviewEligibility> findByUserId(UUID userId);

    @Query("SELECT e.productId FROM ReviewEligibility e WHERE e.userId = :userId AND e.reviewId IS NULL")
    List<Long> findEligibleProductIdsByUserId(@Param("userId") UUID userId);
}

