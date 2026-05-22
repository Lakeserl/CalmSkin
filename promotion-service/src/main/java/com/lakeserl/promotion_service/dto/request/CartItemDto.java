package com.lakeserl.promotion_service.dto.request;

import java.math.BigDecimal;

/** One line of a cart, as sent by order-service to the preview / lock APIs. */
public record CartItemDto(
        Long productId,
        Long variantId,
        Integer quantity,
        BigDecimal unitPrice
) {
}
