package com.lakeserl.notification_service.enums;

/**
 * Delivery channel of a notification. EMAIL and WEB_PUSH are external,
 * IN_APP is persisted in this service and read back through the REST API.
 */
public enum NotificationChannel {
    EMAIL,
    WEB_PUSH,
    IN_APP
}
