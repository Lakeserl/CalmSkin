package com.lakeserl.promotion_service.dto.response;

import java.math.BigDecimal;

/** Usage analytics for one promotion. */
public record PromotionStatsResponse(
        long totalUsageCount,
        BigDecimal totalDiscountGiven,
        BigDecimal avgDiscountAmount
) {
}
