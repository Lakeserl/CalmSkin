package com.lakeserl.promotion_service.dto.response;

import java.math.BigDecimal;

/**
 * Voucher validation result. Field names match the order-service
 * {@code PromotionServiceClient} contract exactly - do not rename.
 */
public record VoucherValidationResponse(
        boolean valid,
        BigDecimal discountAmount,
        String reason
) {
    public static VoucherValidationResponse valid(BigDecimal discountAmount) {
        return new VoucherValidationResponse(true, discountAmount, null);
    }

    public static VoucherValidationResponse invalid(String reason) {
        return new VoucherValidationResponse(false, BigDecimal.ZERO, reason);
    }
}
