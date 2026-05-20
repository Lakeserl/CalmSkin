package com.lakeserl.payment_service.models.enums;

/**
 * Delivery status of a transactional outbox event.
 */
public enum OutboxStatus {
    PENDING,
    SENT,
    FAILED
}
