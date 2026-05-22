package com.lakeserl.promotion_service.enums;

/** Delivery status of a transactional outbox event. */
public enum OutboxStatus {
    PENDING,
    SENT,
    FAILED
}
