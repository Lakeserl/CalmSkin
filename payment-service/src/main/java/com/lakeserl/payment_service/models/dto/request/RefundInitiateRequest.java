package com.lakeserl.payment_service.models.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RefundInitiateRequest(
        @NotNull(message = "Amount is required")
        @Min(value = 1000, message = "Minimum refund amount is 1000 VND")
        Long amount,

        @NotBlank(message = "Reason is required")
        String reason
) {}
