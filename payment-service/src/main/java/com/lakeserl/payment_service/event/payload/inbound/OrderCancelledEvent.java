package com.lakeserl.payment_service.event.payload.inbound;

public record OrderCancelledEvent(
        String orderId,
        String orderNumber,
        Long userId,
        String reason
) {}
