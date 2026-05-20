package com.lakeserl.payment_service.models.enums;

/**
 * Lifecycle of a refund record.
 * PENDING -> PROCESSING -> COMPLETED | FAILED
 */
public enum RefundStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED
}
