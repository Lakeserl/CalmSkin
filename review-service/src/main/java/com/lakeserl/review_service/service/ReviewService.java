package com.lakeserl.review_service.service;

import com.lakeserl.review_service.dto.request.CreateReviewRequest;
import com.lakeserl.review_service.dto.request.UpdateReviewRequest;
import com.lakeserl.review_service.dto.request.AdminUpdateReviewRequest;
import com.lakeserl.review_service.dto.response.ReviewDTO;
import com.lakeserl.review_service.dto.response.AdminReviewStatsDTO;
import com.lakeserl.review_service.enums.ReviewStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ReviewService {

    ReviewDTO createReview(UUID userId, Long productId, CreateReviewRequest request);

    ReviewDTO getReview(Long reviewId, UUID currentUserId);

    Page<ReviewDTO> getProductReviews(Long productId, Short rating, String skinType, Pageable pageable);

    Page<ReviewDTO> getMyReviews(UUID userId, Pageable pageable);

    ReviewDTO updateReview(Long reviewId, UUID userId, UpdateReviewRequest request);

    void deleteReview(Long reviewId, UUID userId);

    // Admin
    Page<ReviewDTO> adminListReviews(ReviewStatus status, Long productId, Pageable pageable);

    ReviewDTO adminUpdateReview(Long reviewId, UUID adminId, AdminUpdateReviewRequest request);

    AdminReviewStatsDTO adminGetStats();

    void hideReviewsForBannedUser(UUID userId);
}


