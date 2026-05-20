package com.lakeserl.order_service.event.payload.inbound;

public record PaymentFailedEvent(
    String orderId,
    String reason
) {}
