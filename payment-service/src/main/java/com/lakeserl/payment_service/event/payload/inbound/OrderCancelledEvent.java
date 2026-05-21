package com.lakeserl.payment_service.event.payload.inbound;

import java.util.UUID;

public record OrderCancelledEvent(
        String orderId,
        String orderNumber,
        UUID userId,
        String reason
) {}
