package com.lakeserl.product_service.repository;

import com.lakeserl.product_service.entity.ReviewSummary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReviewSummaryRepository extends JpaRepository<ReviewSummary, Long> {

    Optional<ReviewSummary> findByProductId(Long productId);
}
