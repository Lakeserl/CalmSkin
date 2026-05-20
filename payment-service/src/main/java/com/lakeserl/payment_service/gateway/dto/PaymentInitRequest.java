package com.lakeserl.payment_service.gateway.dto;

import lombok.Builder;

/**
 * Input to {@link com.lakeserl.payment_service.gateway.PaymentGateway#initiate}.
 * All monetary amounts are in VND (Long).
 */
@Builder
public record PaymentInitRequest(
        String orderNumber,
        Long amount,
        String returnUrl,
        String ipnUrl,
        String orderInfo,
        String ipAddress) {
}
