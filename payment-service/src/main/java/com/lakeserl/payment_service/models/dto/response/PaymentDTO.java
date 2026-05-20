package com.lakeserl.payment_service.models.dto.response;

import java.time.LocalDateTime;

public record PaymentDTO(
        Long id,
        String paymentNumber,
        Long orderId,
        String orderNumber,
        Long userId,
        Long amount,
        Long refundedAmount,
        String method,
        String status,
        String transactionRef,
        String gatewayTransactionId,
        String paymentUrl,
        String failureReason,
        LocalDateTime expiresAt,
        LocalDateTime paidAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
