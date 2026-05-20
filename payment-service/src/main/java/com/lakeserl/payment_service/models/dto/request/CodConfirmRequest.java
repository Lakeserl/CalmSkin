package com.lakeserl.payment_service.models.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CodConfirmRequest(
        @NotBlank(message = "Order number is required")
        String orderNumber
) {}
