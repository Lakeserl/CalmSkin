package com.lakeserl.payment_service.models.dto.response;

import java.time.LocalDateTime;

public record RefundDTO(
        String refundNumber,
        Long paymentId,
        Long orderId,
        Long amount,
        String reason,
        String refundMethod,
        String status,
        String gatewayRefundId,
        String failureReason,
        LocalDateTime processedAt,
        LocalDateTime createdAt
) {}
