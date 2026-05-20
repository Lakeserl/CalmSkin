package com.lakeserl.payment_service.gateway.dto;

import lombok.Builder;

/**
 * Result returned by {@link com.lakeserl.payment_service.gateway.PaymentGateway#refund}.
 */
@Builder
public record RefundResult(
        boolean success,
        String gatewayRefundId,
        String errorMessage) {

    public static RefundResult fail(String errorMessage) {
        return RefundResult.builder()
                .success(false)
                .errorMessage(errorMessage)
                .build();
    }
}
