package com.lakeserl.product_service.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "review_summary",
    indexes = {
        @Index(name = "idx_review_product", columnList = "product_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewSummary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "review_summary_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false, unique = true)
    private Product product;

    @Column(name = "average_rating", precision = 3, scale = 2)
    @Builder.Default
    private BigDecimal averageRating = BigDecimal.ZERO;

    @Column(name = "total_reviews", nullable = false)
    @Builder.Default
    private Integer totalReviews = 0;

    @Column(name = "five_star_count", nullable = false)
    @Builder.Default
    private Integer fiveStarCount = 0;

    @Column(name = "four_star_count", nullable = false)
    @Builder.Default
    private Integer fourStarCount = 0;

    @Column(name = "three_star_count", nullable = false)
    @Builder.Default
    private Integer threeStarCount = 0;

    @Column(name = "two_star_count", nullable = false)
    @Builder.Default
    private Integer twoStarCount = 0;

    @Column(name = "one_star_count", nullable = false)
    @Builder.Default
    private Integer oneStarCount = 0;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
