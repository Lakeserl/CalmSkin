package com.lakeserl.order_service.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateOrderInternalRequest(
    @NotNull(message = "User ID is required")
    UUID userId,

    @NotNull(message = "Order request details are required")
    @Valid
    CreateOrderRequest orderRequest
) {}
