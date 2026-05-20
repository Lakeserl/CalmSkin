package com.lakeserl.payment_service.models.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PaymentInitiateRequest(
        @NotBlank(message = "Order number is required")
        String orderNumber,

        @NotBlank(message = "Payment method is required")
        String paymentMethod
) {}
