package com.lakeserl.promotion_service.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** A voucher assigned to the calling user. */
public record MyVoucherResponse(
        Long promotionId,
        String code,
        String name,
        BigDecimal discountValue,
        LocalDateTime endsAt,
        long usedCount,
        Integer usageLimit
) {
}
