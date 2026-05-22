package com.lakeserl.promotion_service.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Public preview of a voucher code (no apply, no lock). */
public record VoucherInfoResponse(
        String code,
        String name,
        String discountType,
        BigDecimal discountValue,
        BigDecimal minOrderValue,
        LocalDateTime endsAt,
        boolean isValid,
        String reason
) {
}
