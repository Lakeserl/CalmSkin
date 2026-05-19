package com.lakeserl.inventory_service.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CheckStockRequest(
        @NotNull Long productId,
        Long variantId,
        @NotNull @Min(0) Integer quantity
) {
}
