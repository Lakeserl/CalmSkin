package com.lakeserl.promotion_service.dto.request;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

/** Request to lock promotions for an order being created. */
public record LockRequest(
        @NotNull String orderId,
        @NotNull UUID userId,
        @NotEmpty List<Long> promotionIds,
        List<CartItemDto> cartItems,
        BigDecimal cartTotal
) {
}
