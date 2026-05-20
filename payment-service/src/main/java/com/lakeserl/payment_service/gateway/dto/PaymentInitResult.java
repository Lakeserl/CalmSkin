package com.lakeserl.payment_service.gateway.dto;

import lombok.Builder;

/**
 * Result returned by {@link com.lakeserl.payment_service.gateway.PaymentGateway#initiate}.
 */
@Builder
public record PaymentInitResult(
        String paymentUrl,
        String transactionRef,
        boolean success,
        String errorMessage) {

    public static PaymentInitResult fail(String errorMessage) {
        return PaymentInitResult.builder()
                .success(false)
                .errorMessage(errorMessage)
                .build();
    }
}
