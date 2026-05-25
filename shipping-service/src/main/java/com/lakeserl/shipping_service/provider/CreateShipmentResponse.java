package com.lakeserl.shipping_service.provider;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record CreateShipmentResponse(
        String providerOrderId,
        String trackingNumber,
        LocalDateTime estimatedPickupAt,
        LocalDateTime estimatedDeliveryAt
) {}
