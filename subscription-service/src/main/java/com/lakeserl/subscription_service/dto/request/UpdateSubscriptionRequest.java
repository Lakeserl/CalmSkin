package com.lakeserl.subscription_service.dto.request;

import jakarta.validation.constraints.Min;

import java.util.UUID;

/**
 * All fields are optional — only non-null values will be applied.
 */
public record UpdateSubscriptionRequest(
        @Min(value = 1, message = "frequencyDays must be at least 1")
        Integer frequencyDays,

        UUID addressId
) {}
