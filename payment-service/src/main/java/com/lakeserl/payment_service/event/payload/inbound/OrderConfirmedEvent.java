package com.lakeserl.payment_service.event.payload.inbound;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderConfirmedEvent(
        String orderId,
        String orderNumber,
        UUID userId,
        BigDecimal totalAmount,
        String paymentMethod
) {}
