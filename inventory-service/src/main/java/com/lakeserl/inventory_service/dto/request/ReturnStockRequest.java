package com.lakeserl.inventory_service.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record ReturnStockRequest(
        @NotBlank String orderId,
        @NotEmpty List<@Valid ReturnItem> items
) {
    public record ReturnItem(
            @NotNull Long productId,
            Long variantId,
            @NotNull @Min(1) Integer quantity
    ) {
    }
}
