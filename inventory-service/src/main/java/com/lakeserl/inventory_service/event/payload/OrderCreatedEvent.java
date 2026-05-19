package com.lakeserl.inventory_service.event.payload;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record OrderCreatedEvent(
        @NotBlank String orderId,
        @NotEmpty List<OrderItem> items
) {
    public record OrderItem(
            @NotNull Long productId,
            Long variantId,
            @NotNull @Min(1) Integer quantity
    ) {
    }
}
