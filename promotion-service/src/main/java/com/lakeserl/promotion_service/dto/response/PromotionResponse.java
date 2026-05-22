package com.lakeserl.promotion_service.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** Full promotion view for admin endpoints. */
public record PromotionResponse(
        Long id,
        String code,
        String name,
        String description,
        String type,
        String discountType,
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
        String status,
        boolean stackable,
        Integer priority,
        UUID createdBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
