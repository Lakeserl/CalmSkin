package com.lakeserl.shipping_service.dto.request;

import com.lakeserl.shipping_service.enums.ShipmentStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

// Generic webhook payload normalized from any carrier. Carrier-specific
// adapters translate their payload into this shape before invoking the service.
public record WebhookEventRequest(
        @NotBlank String trackingNumber,
        @NotNull ShipmentStatus status,
        String description,
        String location,
        LocalDateTime occurredAt
) {}
