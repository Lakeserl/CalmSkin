package com.lakeserl.promotion_service.dto.request;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Admin update of a promotion's scalar fields. Null fields are left unchanged.
 * Child rows (tiers / flash sales / bundle items) are not edited here.
 */
public record UpdatePromotionRequest(
        String name,
        String description,
        BigDecimal discountValue,
        BigDecimal maxDiscountAmount,
        BigDecimal minOrderValue,
        Integer minItemQuantity,
        List<Long> applicableProductIds,
        List<Long> applicableCategoryIds,
        List<Long> applicableBrandIds,
        List<Long> excludedProductIds,
        Integer totalUsageLimit,
        Integer perUserLimit,
        LocalDateTime startsAt,
        LocalDateTime endsAt,
        Boolean isStackable,
        Integer priority
) {
}
