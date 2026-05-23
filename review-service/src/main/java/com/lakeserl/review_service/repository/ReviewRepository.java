package com.lakeserl.review_service.repository;

import com.lakeserl.review_service.entity.Review;
import com.lakeserl.review_service.enums.ReviewStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    Page<Review> findByProductIdAndStatus(Long productId, ReviewStatus status, Pageable pageable);

    @Query("SELECT r FROM Review r WHERE r.productId = :productId AND r.status = :status " +
           "AND (:rating IS NULL OR r.rating = :rating) " +
           "AND (:skinType IS NULL OR r.skinType = :skinType)")
    Page<Review> findByFilters(@Param("productId") Long productId,
                               @Param("status") ReviewStatus status,
                               @Param("rating") Short rating,
                               @Param("skinType") String skinType,
                               Pageable pageable);

    Page<Review> findByUserId(UUID userId, Pageable pageable);

    Optional<Review> findByIdAndUserId(Long id, UUID userId);

    boolean existsByUserIdAndOrderItemId(UUID userId, Long orderItemId);

    @Query("SELECT r FROM Review r WHERE r.status = 'PUBLISHED' AND r.reportCount >= :threshold")
    List<Review> findHighReportCountPublished(@Param("threshold") int threshold);

    @Query("SELECT r FROM Review r WHERE r.userId = :userId AND r.status != 'DELETED'")
    Page<Review> findActiveByUserId(@Param("userId") UUID userId, Pageable pageable);

    @Query("SELECT COUNT(r) FROM Review r WHERE r.productId = :productId AND r.status = 'PUBLISHED'")
    long countPublishedByProductId(@Param("productId") Long productId);

    List<Review> findByUserIdAndStatus(UUID userId, ReviewStatus status);
}


