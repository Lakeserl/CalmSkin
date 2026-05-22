package com.lakeserl.promotion_service.dto.request;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Cart preview request (read-only - no locks taken). */
public record PreviewRequest(
        UUID userId,
        List<CartItemDto> cartItems,
        List<String> voucherCodes,
        BigDecimal cartTotal,
        BigDecimal shippingFee
) {
}
