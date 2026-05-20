package com.lakeserl.payment_service.models.enums;

/**
 * Lifecycle of a payment record.
 * PENDING -> COMPLETED | FAILED | CANCELLED | EXPIRED
 * COMPLETED -> PARTIALLY_REFUNDED -> REFUNDED
 */
public enum PaymentStatus {
    PENDING,
    COMPLETED,
    FAILED,
    CANCELLED,
    EXPIRED,
    REFUNDED,
    PARTIALLY_REFUNDED
}
