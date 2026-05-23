package com.lakeserl.review_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "product_review_summary")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductReviewSummary {

    @Id
    @Column(name = "product_id")
    private Long productId;

    @Column(name = "total_count", nullable = false)
    @Builder.Default
    private int totalCount = 0;

    @Column(name = "average_rating", nullable = false, precision = 3, scale = 2)
    @Builder.Default
    private BigDecimal averageRating = BigDecimal.ZERO;

    @Column(name = "count_1star", nullable = false)
    @Builder.Default
    private int count1star = 0;

    @Column(name = "count_2star", nullable = false)
    @Builder.Default
    private int count2star = 0;

    @Column(name = "count_3star", nullable = false)
    @Builder.Default
    private int count3star = 0;

    @Column(name = "count_4star", nullable = false)
    @Builder.Default
    private int count4star = 0;

    @Column(name = "count_5star", nullable = false)
    @Builder.Default
    private int count5star = 0;

    // Skin type breakdown
    @Column(name = "count_oily", nullable = false)
    @Builder.Default
    private int countOily = 0;

    @Column(name = "count_dry", nullable = false)
    @Builder.Default
    private int countDry = 0;

    @Column(name = "count_combination", nullable = false)
    @Builder.Default
    private int countCombination = 0;

    @Column(name = "count_sensitive", nullable = false)
    @Builder.Default
    private int countSensitive = 0;

    @Column(name = "count_normal", nullable = false)
    @Builder.Default
    private int countNormal = 0;

    // Detailed average ratings
    @Column(name = "avg_skin_effect", precision = 3, scale = 2)
    private BigDecimal avgSkinEffect;

    @Column(name = "avg_texture", precision = 3, scale = 2)
    private BigDecimal avgTexture;

    @Column(name = "avg_scent", precision = 3, scale = 2)
    private BigDecimal avgScent;

    @Column(name = "avg_packaging", precision = 3, scale = 2)
    private BigDecimal avgPackaging;

    @Column(name = "avg_value", precision = 3, scale = 2)
    private BigDecimal avgValue;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
