package com.lakeserl.payment_service.gateway.dto;

import lombok.Builder;

/**
 * Input to {@link com.lakeserl.payment_service.gateway.PaymentGateway#refund}.
 * Amount is in VND (Long).
 */
@Builder
public record RefundGatewayRequest(
        String transactionId,
        Long amount,
        String refundRef,
        String reason,
        String ipAddress,
        String transactionDate) {
}
