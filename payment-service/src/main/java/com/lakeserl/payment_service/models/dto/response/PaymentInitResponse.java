package com.lakeserl.payment_service.models.dto.response;

public record PaymentInitResponse(
        String paymentNumber,
        String paymentUrl,
        String status
) {}
