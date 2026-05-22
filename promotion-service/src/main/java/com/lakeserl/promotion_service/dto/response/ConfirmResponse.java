package com.lakeserl.promotion_service.dto.response;

/** Result of confirming an order's locked promotions. */
public record ConfirmResponse(
        boolean confirmed
) {
}
