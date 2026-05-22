package com.lakeserl.promotion_service.dto.response;

/** Result of releasing an order's locked promotions. */
public record ReleaseResponse(
        boolean released
) {
}
