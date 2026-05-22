package com.lakeserl.promotion_service.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import com.lakeserl.promotion_service.enums.DiscountType;
import com.lakeserl.promotion_service.enums.PromotionStatus;
import com.lakeserl.promotion_service.enums.PromotionType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A promotion definition. Scope fields ({@code applicable*Ids}, {@code
 * excludedProductIds}) hold a comma-separated list of ids; blank/null means "no
 * restriction". Money fields are NUMERIC(15,2).
 */
@Entity
@Table(name = "promotions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Promotion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", length = 50, unique = true)
    private String code;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 30)
    private PromotionType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false, length = 20)
    private DiscountType discountType;

    @Column(name = "discount_value", nullable = false, precision = 15, scale = 2)
    private BigDecimal discountValue;

    @Column(name = "max_discount_amount", precision = 15, scale = 2)
    private BigDecimal maxDiscountAmount;

    @Column(name = "min_order_value", nullable = false, precision = 15, scale = 2)
    private BigDecimal minOrderValue;

    @Column(name = "min_item_quantity", nullable = false)
    private Integer minItemQuantity;

    @Column(name = "applicable_product_ids", columnDefinition = "TEXT")
    private String applicableProductIds;

    @Column(name = "applicable_category_ids", columnDefinition = "TEXT")
    private String applicableCategoryIds;

    @Column(name = "applicable_brand_ids", columnDefinition = "TEXT")
    private String applicableBrandIds;

    @Column(name = "excluded_product_ids", columnDefinition = "TEXT")
    private String excludedProductIds;

    @Column(name = "total_usage_limit")
    private Integer totalUsageLimit;

    @Column(name = "per_user_limit", nullable = false)
    private Integer perUserLimit;

    @Column(name = "starts_at", nullable = false)
    private LocalDateTime startsAt;

    @Column(name = "ends_at", nullable = false)
    private LocalDateTime endsAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PromotionStatus status;

    @Column(name = "is_stackable", nullable = false)
    private Boolean isStackable;

    @Column(name = "priority", nullable = false)
    private Integer priority;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (status == null) {
            status = PromotionStatus.DRAFT;
        }
        if (isStackable == null) {
            isStackable = Boolean.FALSE;
        }
        if (priority == null) {
            priority = 0;
        }
        if (minItemQuantity == null) {
            minItemQuantity = 0;
        }
        if (perUserLimit == null) {
            perUserLimit = 1;
        }
        if (minOrderValue == null) {
            minOrderValue = BigDecimal.ZERO;
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
