package com.lakeserl.review_service.repository;

import com.lakeserl.review_service.entity.ReviewReport;
import com.lakeserl.review_service.enums.ReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ReviewReportRepository extends JpaRepository<ReviewReport, Long> {
    boolean existsByReviewIdAndReporterId(Long reviewId, UUID reporterId);
    Optional<ReviewReport> findByReviewIdAndReporterId(Long reviewId, UUID reporterId);
    Page<ReviewReport> findByStatus(ReportStatus status, Pageable pageable);
}

