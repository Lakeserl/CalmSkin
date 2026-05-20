package com.lakeserl.payment_service.event.payload.outbound;

import java.math.BigDecimal;

public record PaymentRefundedEvent(
        String orderId,
        String refundNumber,
        BigDecimal amount,
        String reason
) {}
