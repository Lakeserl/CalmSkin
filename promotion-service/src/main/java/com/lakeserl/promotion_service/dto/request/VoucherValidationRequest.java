package com.lakeserl.promotion_service.dto.request;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Voucher validation request. Field names match the existing order-service
 * {@code PromotionServiceClient} contract exactly - do not rename.
 */
public record VoucherValidationRequest(
        String voucherCode,
        BigDecimal subtotal,
        UUID userId
) {
}
