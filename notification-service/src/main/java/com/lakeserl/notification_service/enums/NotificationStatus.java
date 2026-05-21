package com.lakeserl.notification_service.enums;

/**
 * Lifecycle status of a single notification row.
 * <ul>
 *   <li>SCHEDULED - waiting for {@code scheduled_at} to elapse.</li>
 *   <li>PENDING   - due to send / re-send, picked up by the sender scheduler.</li>
 *   <li>SENT      - delivered (for IN_APP this means persisted to the feed).</li>
 *   <li>FAILED    - channel send failed, eligible for retry.</li>
 *   <li>READ      - an IN_APP notification the user has opened.</li>
 * </ul>
 */
public enum NotificationStatus {
    SCHEDULED,
    PENDING,
    SENT,
    FAILED,
    READ
}
