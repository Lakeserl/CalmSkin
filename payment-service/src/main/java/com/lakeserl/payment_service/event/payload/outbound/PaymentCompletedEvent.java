package com.lakeserl.payment_service.event.payload.outbound;

import java.math.BigDecimal;

public record PaymentCompletedEvent(
        String orderId,
        String transactionId,
        BigDecimal amount,
        String paymentMethod
) {}
