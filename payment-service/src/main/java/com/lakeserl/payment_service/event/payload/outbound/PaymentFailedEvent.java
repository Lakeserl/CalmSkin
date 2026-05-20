package com.lakeserl.payment_service.event.payload.outbound;

public record PaymentFailedEvent(
        String orderId,
        String reason
) {}
