package com.lakeserl.review_service.service;

import com.lakeserl.review_service.dto.request.CreateReportRequest;
import com.lakeserl.review_service.dto.request.AdminUpdateReportRequest;
import com.lakeserl.review_service.dto.response.ReviewReportDTO;
import com.lakeserl.review_service.entity.Review;
import com.lakeserl.review_service.entity.ReviewReport;
import com.lakeserl.review_service.enums.ReportStatus;
import com.lakeserl.review_service.exception.AlreadyReviewedException;
import com.lakeserl.review_service.exception.ReviewNotFoundException;
import com.lakeserl.review_service.repository.ReviewRepository;
import com.lakeserl.review_service.repository.ReviewReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReviewReportService {

    private static final int AUTO_HIDE_THRESHOLD = 5;

    private final ReviewRepository reviewRepository;
    private final ReviewReportRepository reportRepository;

    @Transactional
    public ReviewReportDTO createReport(Long reviewId, UUID reporterId, CreateReportRequest request) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewNotFoundException("Review " + reviewId + " not found."));

        if (reportRepository.existsByReviewIdAndReporterId(reviewId, reporterId)) {
            throw new AlreadyReviewedException("You have already reported this review.");
        }

        ReviewReport report = ReviewReport.builder()
                .review(review)
                .reporterId(reporterId)
                .reason(request.reason())
                .detail(request.detail())
                .status(ReportStatus.PENDING)
                .build();
        reportRepository.save(report);

        // Auto-hide if report count hits threshold
        review.setReportCount(review.getReportCount() + 1);
        if (review.getReportCount() >= AUTO_HIDE_THRESHOLD) {
            review.setStatus(com.lakeserl.review_service.enums.ReviewStatus.PENDING_MODERATION);
        }
        reviewRepository.save(review);

        return toDTO(report);
    }

    @Transactional(readOnly = true)
    public Page<ReviewReportDTO> adminListReports(ReportStatus status, Pageable pageable) {
        return reportRepository.findByStatus(status, pageable).map(this::toDTO);
    }

    @Transactional
    public ReviewReportDTO adminUpdateReport(Long reportId, UUID adminId, AdminUpdateReportRequest request) {
        ReviewReport report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ReviewNotFoundException("Report " + reportId + " not found."));
        report.setStatus(request.status());
        report.setResolvedBy(adminId);
        report.setResolvedAt(LocalDateTime.now());
        reportRepository.save(report);
        return toDTO(report);
    }

    private ReviewReportDTO toDTO(ReviewReport report) {
        return ReviewReportDTO.builder()
                .id(report.getId())
                .reviewId(report.getReview().getId())
                .reporterId(report.getReporterId())
                .reason(report.getReason())
                .detail(report.getDetail())
                .status(report.getStatus())
                .createdAt(report.getCreatedAt())
                .build();
    }
}

