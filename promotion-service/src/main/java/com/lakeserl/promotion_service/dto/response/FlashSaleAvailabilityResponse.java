package com.lakeserl.promotion_service.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Flash-sale availability for a product/variant. */
public record FlashSaleAvailabilityResponse(
        boolean hasFlashSale,
        BigDecimal salePrice,
        Integer quantityLeft,
        LocalDateTime endsAt
) {
    public static FlashSaleAvailabilityResponse none() {
        return new FlashSaleAvailabilityResponse(false, null, null, null);
    }
}
