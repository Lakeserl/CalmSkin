package com.lakeserl.order_service.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;
import java.util.UUID;

public record CreateOrderRequest(
    @NotEmpty(message = "Order items list must not be empty")
    @Size(max = 50, message = "Order cannot contain more than 50 items")
    List<@Valid OrderItemRequest> items,

    @NotNull(message = "Address ID is required")
    UUID addressId,

    @NotBlank(message = "Payment method is required")
    String paymentMethod,

    @Size(max = 50, message = "Voucher code must not exceed 50 characters")
    String voucherCode,

    @Min(value = 0, message = "Points to use must be non-negative")
    Integer pointsToUse,

    @Size(max = 500, message = "Note must not exceed 500 characters")
    String note
) {
    public record OrderItemRequest(
        @NotNull(message = "Product ID is required")
        @Positive(message = "Product ID must be positive")
        Long productId,

        Long variantId,

        @NotNull(message = "Quantity is required")
        @Min(value = 1, message = "Quantity must be at least 1")
        @Max(value = 99, message = "Quantity cannot exceed 99")
        Integer quantity
    ) {}
}
