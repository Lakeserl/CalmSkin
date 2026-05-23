package com.lakeserl.review_service.service;

import com.lakeserl.review_service.dto.request.*;
import com.lakeserl.review_service.dto.response.*;
import com.lakeserl.review_service.entity.*;
import com.lakeserl.review_service.enums.OutboxStatus;
import com.lakeserl.review_service.enums.ReviewStatus;
import com.lakeserl.review_service.exception.*;
import com.lakeserl.review_service.repository.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private static final int EDIT_WINDOW_DAYS = 30;
    private static final int MAX_MEDIA_PER_REVIEW = 5;

    private final ReviewRepository reviewRepository;
    private final ReviewMediaRepository reviewMediaRepository;
    private final ReviewEligibilityRepository eligibilityRepository;
    private final OutboxRepository outboxRepository;
    private final ReviewSummaryService summaryService;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public ReviewDTO createReview(UUID userId, Long productId, CreateReviewRequest request) {
        // 1. Check eligibility — verified purchase via order.completed event
        ReviewEligibility eligibility = eligibilityRepository
                .findByUserIdAndOrderItemId(userId, request.orderItemId())
                .orElseThrow(() -> new NotEligibleToReviewException(
                        "You must purchase this product before reviewing it."));

        // 2. Guard against duplicate review for the same order item
        if (eligibility.getReviewId() != null) {
            throw new AlreadyReviewedException("You have already reviewed this order item.");
        }

        // 3. Create Review entity
        Review review = Review.builder()
                .productId(productId)
                .userId(userId)
                .orderId(eligibility.getOrderItemId()) // stored as reference
                .orderItemId(request.orderItemId())
                .rating(request.rating())
                .title(request.title())
                .body(request.body())
                .skinType(request.skinType())
                .ageRange(request.ageRange())
                .skinEffectRating(request.skinEffectRating())
                .textureRating(request.textureRating())
                .scentRating(request.scentRating())
                .packagingRating(request.packagingRating())
                .valueRating(request.valueRating())
                .verified(true)
                .status(ReviewStatus.PUBLISHED)
                .build();

        reviewRepository.save(review);

        // 4. Save media
        if (request.mediaUrls() != null && !request.mediaUrls().isEmpty()) {
            saveMediaForReview(review, request.mediaUrls());
        }

        // 5. Mark eligibility as used
        eligibility.setReviewId(review.getId());
        eligibilityRepository.save(eligibility);

        // 6. Rebuild summary (async)
        summaryService.rebuildSummary(productId);

        // 7. Outbox event
        publishOutboxEvent("Review", review.getId().toString(), "review.created",
                Map.of("reviewId", review.getId(), "productId", productId, "userId", userId.toString(),
                       "rating", review.getRating()));

        return toDTO(review, null);
    }

    @Override
    @Transactional(readOnly = true)
    public ReviewDTO getReview(Long reviewId, UUID currentUserId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewNotFoundException("Review " + reviewId + " not found."));
        List<ReviewMedia> media = reviewMediaRepository.findByReviewIdOrderBySortOrderAsc(reviewId);
        return toDTO(review, media);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReviewDTO> getProductReviews(Long productId, Short rating, String skinType, Pageable pageable) {
        Page<Review> reviews = reviewRepository.findByFilters(
                productId, ReviewStatus.PUBLISHED, rating, skinType, pageable);
        return reviews.map(r -> toDTO(r, reviewMediaRepository.findByReviewIdOrderBySortOrderAsc(r.getId())));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReviewDTO> getMyReviews(UUID userId, Pageable pageable) {
        return reviewRepository.findActiveByUserId(userId, pageable)
                .map(r -> toDTO(r, reviewMediaRepository.findByReviewIdOrderBySortOrderAsc(r.getId())));
    }

    @Override
    @Transactional
    public ReviewDTO updateReview(Long reviewId, UUID userId, UpdateReviewRequest request) {
        Review review = reviewRepository.findByIdAndUserId(reviewId, userId)
                .orElseThrow(() -> new ReviewNotFoundException("Review not found or does not belong to you."));

        if (review.getCreatedAt().isBefore(LocalDateTime.now().minusDays(EDIT_WINDOW_DAYS))) {
            throw new ReviewEditWindowExpiredException("Reviews can only be edited within " + EDIT_WINDOW_DAYS + " days of posting.");
        }

        if (request.rating() != null) review.setRating(request.rating());
        if (request.title() != null) review.setTitle(request.title());
        if (request.body() != null) review.setBody(request.body());
        if (request.skinType() != null) review.setSkinType(request.skinType());
        if (request.ageRange() != null) review.setAgeRange(request.ageRange());
        if (request.skinEffectRating() != null) review.setSkinEffectRating(request.skinEffectRating());
        if (request.textureRating() != null) review.setTextureRating(request.textureRating());
        if (request.scentRating() != null) review.setScentRating(request.scentRating());
        if (request.packagingRating() != null) review.setPackagingRating(request.packagingRating());
        if (request.valueRating() != null) review.setValueRating(request.valueRating());

        if (request.mediaUrls() != null) {
            reviewMediaRepository.deleteAll(reviewMediaRepository.findByReviewIdOrderBySortOrderAsc(reviewId));
            saveMediaForReview(review, request.mediaUrls());
        }

        reviewRepository.save(review);
        summaryService.rebuildSummary(review.getProductId());

        return toDTO(review, reviewMediaRepository.findByReviewIdOrderBySortOrderAsc(reviewId));
    }

    @Override
    @Transactional
    public void deleteReview(Long reviewId, UUID userId) {
        Review review = reviewRepository.findByIdAndUserId(reviewId, userId)
                .orElseThrow(() -> new ReviewNotFoundException("Review not found or does not belong to you."));
        review.setStatus(ReviewStatus.DELETED);
        reviewRepository.save(review);
        summaryService.rebuildSummary(review.getProductId());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReviewDTO> adminListReviews(ReviewStatus status, Long productId, Pageable pageable) {
        if (productId != null && status != null) {
            return reviewRepository.findByProductIdAndStatus(productId, status, pageable)
                    .map(r -> toDTO(r, null));
        }
        if (status != null) {
            return reviewRepository.findAll(pageable).map(r -> toDTO(r, null));
        }
        return reviewRepository.findAll(pageable).map(r -> toDTO(r, null));
    }

    @Override
    @Transactional
    public ReviewDTO adminUpdateReview(Long reviewId, UUID adminId, AdminUpdateReviewRequest request) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewNotFoundException("Review " + reviewId + " not found."));
        review.setStatus(request.status());
        review.setAdminNote(request.adminNote());
        review.setModeratedBy(adminId);
        review.setModeratedAt(LocalDateTime.now());
        reviewRepository.save(review);
        summaryService.rebuildSummary(review.getProductId());
        return toDTO(review, null);
    }

    @Override
    @Transactional(readOnly = true)
    public AdminReviewStatsDTO adminGetStats() {
        long total = reviewRepository.count();
        long published = reviewRepository.findAll().stream()
                .filter(r -> r.getStatus() == ReviewStatus.PUBLISHED).count();
        long pending = reviewRepository.findAll().stream()
                .filter(r -> r.getStatus() == ReviewStatus.PENDING_MODERATION).count();
        long hidden = reviewRepository.findAll().stream()
                .filter(r -> r.getStatus() == ReviewStatus.HIDDEN).count();
        long deleted = reviewRepository.findAll().stream()
                .filter(r -> r.getStatus() == ReviewStatus.DELETED).count();
        return AdminReviewStatsDTO.builder()
                .totalReviews(total)
                .publishedCount(published)
                .pendingModerationCount(pending)
                .hiddenCount(hidden)
                .deletedCount(deleted)
                .build();
    }

    // ────────── private helpers ──────────

    private void saveMediaForReview(Review review, List<String> mediaUrls) {
        for (short i = 0; i < mediaUrls.size(); i++) {
            String url = mediaUrls.get(i);
            com.lakeserl.review_service.enums.MediaType type =
                    url.matches(".*\\.(mp4|mov|avi|webm)(\\?.*)?") ?
                            com.lakeserl.review_service.enums.MediaType.VIDEO :
                            com.lakeserl.review_service.enums.MediaType.IMAGE;
            ReviewMedia media = ReviewMedia.builder()
                    .review(review)
                    .mediaType(type)
                    .url(url)
                    .sortOrder(i)
                    .build();
            reviewMediaRepository.save(media);
        }
    }

    private void publishOutboxEvent(String aggregateType, String aggregateId, String eventType, Object payload) {
        try {
            String body = objectMapper.writeValueAsString(payload);
            OutboxEvent event = OutboxEvent.builder()
                    .aggregateType(aggregateType)
                    .aggregateId(aggregateId)
                    .eventType(eventType)
                    .payload(body)
                    .status(OutboxStatus.PENDING)
                    .retryCount(0)
                    .build();
            outboxRepository.save(event);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize outbox event for {}", eventType, e);
        }
    }

    private ReviewDTO toDTO(Review review, List<ReviewMedia> media) {
        List<ReviewMediaDTO> mediaDTOs = media == null ? List.of() :
                media.stream().map(m -> ReviewMediaDTO.builder()
                        .id(m.getId())
                        .mediaType(m.getMediaType())
                        .url(m.getUrl())
                        .thumbnailUrl(m.getThumbnailUrl())
                        .sortOrder(m.getSortOrder())
                        .build()).collect(Collectors.toList());

        return ReviewDTO.builder()
                .id(review.getId())
                .productId(review.getProductId())
                .userId(review.getUserId())
                .orderId(review.getOrderId())
                .orderItemId(review.getOrderItemId())
                .rating(review.getRating())
                .title(review.getTitle())
                .body(review.getBody())
                .skinType(review.getSkinType())
                .ageRange(review.getAgeRange())
                .skinEffectRating(review.getSkinEffectRating())
                .textureRating(review.getTextureRating())
                .scentRating(review.getScentRating())
                .packagingRating(review.getPackagingRating())
                .valueRating(review.getValueRating())
                .verified(review.isVerified())
                .status(review.getStatus())
                .helpfulCount(review.getHelpfulCount())
                .notHelpfulCount(review.getNotHelpfulCount())
                .reportCount(review.getReportCount())
                .createdAt(review.getCreatedAt())
                .updatedAt(review.getUpdatedAt())
                .media(mediaDTOs)
                .build();
    }

    @Override
    @Transactional
    public void hideReviewsForBannedUser(UUID userId) {
        log.info("Hiding all reviews for banned user: {}", userId);
        List<Review> publishedReviews = reviewRepository.findByUserIdAndStatus(userId, ReviewStatus.PUBLISHED);
        if (publishedReviews.isEmpty()) {
            return;
        }

        for (Review review : publishedReviews) {
            review.setStatus(ReviewStatus.HIDDEN);
        }
        reviewRepository.saveAll(publishedReviews);

        // Collect unique productIds to rebuild their summaries
        List<Long> productIds = publishedReviews.stream()
                .map(Review::getProductId)
                .distinct()
                .collect(Collectors.toList());

        for (Long productId : productIds) {
            summaryService.rebuildSummary(productId);
        }
    }
}


