package com.lakeserl.order_service.event.payload.inbound;

import java.math.BigDecimal;

public record PaymentCompletedEvent(
    String orderId,
    String transactionId,
    BigDecimal amount,
    String paymentMethod
) {}
