package com.lakeserl.payment_service.gateway.dto;

import lombok.Builder;

/**
 * Result of parsing and verifying an inbound webhook (IPN) from a payment gateway.
 */
@Builder
public record WebhookVerifyResult(
        boolean signatureValid,
        boolean success,
        String transactionId,
        Long amount,
        String transactionRef,
        String rawResponse) {
}
