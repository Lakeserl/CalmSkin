package com.lakeserl.promotion_service.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Lightweight promotion view for the public "active promotions" list. */
public record PromotionSummaryResponse(
        Long id,
        String name,
        String description,
        String type,
        String discountType,
        BigDecimal discountValue,
        BigDecimal minOrderValue,
        LocalDateTime startsAt,
        LocalDateTime endsAt
) {
}
