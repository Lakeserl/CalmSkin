package com.lakeserl.payment_service.event.payload.inbound;

import java.math.BigDecimal;

public record OrderConfirmedEvent(
        String orderId,
        String orderNumber,
        Long userId,
        BigDecimal totalAmount,
        String paymentMethod
) {}
