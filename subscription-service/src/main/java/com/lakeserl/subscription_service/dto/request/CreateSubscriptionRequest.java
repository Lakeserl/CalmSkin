package com.lakeserl.subscription_service.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateSubscriptionRequest(
        @NotNull(message = "productId is required")
        Long productId,

        @NotNull(message = "frequencyDays is required")
        @Min(value = 1, message = "frequencyDays must be at least 1")
        Integer frequencyDays,

        @NotNull(message = "addressId is required")
        UUID addressId
) {}
