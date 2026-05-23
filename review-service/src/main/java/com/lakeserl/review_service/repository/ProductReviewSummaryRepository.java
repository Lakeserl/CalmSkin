package com.lakeserl.review_service.repository;

import com.lakeserl.review_service.entity.ProductReviewSummary;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductReviewSummaryRepository extends JpaRepository<ProductReviewSummary, Long> {
}
