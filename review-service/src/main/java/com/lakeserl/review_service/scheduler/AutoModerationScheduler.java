package com.lakeserl.review_service.scheduler;

import com.lakeserl.review_service.entity.Review;
import com.lakeserl.review_service.enums.ReviewStatus;
import com.lakeserl.review_service.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Automatically flags reviews that have accumulated many reports
 * and re-checks if previously flagged reviews are still valid.
 * Runs every 5 minutes.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AutoModerationScheduler {

    private static final int REPORT_THRESHOLD = 5;

    private final ReviewRepository reviewRepository;

    @Scheduled(fixedDelay = 300_000) // every 5 minutes
    @Transactional
    public void autoFlagHighReportReviews() {
        List<Review> highReportReviews = reviewRepository.findHighReportCountPublished(REPORT_THRESHOLD);
        for (Review review : highReportReviews) {
            if (review.getStatus() == ReviewStatus.PUBLISHED) {
                review.setStatus(ReviewStatus.PENDING_MODERATION);
                reviewRepository.save(review);
                log.info("Auto-flagged reviewId={} (reportCount={})", review.getId(), review.getReportCount());
            }
        }
    }
}
