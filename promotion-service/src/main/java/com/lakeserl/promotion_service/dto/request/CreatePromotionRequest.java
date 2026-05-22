package com.lakeserl.promotion_service.dto.request;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.lakeserl.promotion_service.enums.DiscountType;
import com.lakeserl.promotion_service.enums.PromotionType;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Admin request to create a promotion. Created in DRAFT status. Optional
 * {@code tiers} / {@code flashSales} / {@code bundleItems} / {@code gifts}
 * attach the child rows for tiered / flash-sale / bundle / free-gift promotions.
 */
public record CreatePromotionRequest(
        String code,
        @NotNull String name,
        String description,
        @NotNull PromotionType type,
        @NotNull DiscountType discountType,
        @NotNull @Positive BigDecimal discountValue,
        BigDecimal maxDiscountAmount,
        BigDecimal minOrderValue,
        Integer minItemQuantity,
        List<Long> applicableProductIds,
        List<Long> applicableCategoryIds,
        List<Long> applicableBrandIds,
        List<Long> excludedProductIds,
        Integer totalUsageLimit,
        Integer perUserLimit,
        @NotNull LocalDateTime startsAt,
        @NotNull LocalDateTime endsAt,
        Boolean isStackable,
        Integer priority,
        List<TierInput> tiers,
        List<FlashSaleInput> flashSales,
        List<BundleInput> bundleItems,
        List<GiftInput> gifts
) {

    public record TierInput(
            Integer minQuantity,
            BigDecimal minValue,
            DiscountType discountType,
            BigDecimal discountValue,
            Short sortOrder
    ) {
    }

    public record FlashSaleInput(
            Long productId,
            Long variantId,
            BigDecimal originalPrice,
            BigDecimal salePrice,
            Integer quantityLimit
    ) {
    }

    public record BundleInput(
            Long productId,
            Long variantId,
            Integer quantity
    ) {
    }

    public record GiftInput(
            Long productId,
            Long variantId,
            Integer quantity
    ) {
    }
}
