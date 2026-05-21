package com.lakeserl.notification_service.enums;

/**
 * Priority lane of a notification. Each lane has its own Kafka topic so that a
 * flood of bulk traffic can never delay a CRITICAL security message.
 * CRITICAL and HIGH bypass quiet hours.
 */
public enum NotificationPriority {

    CRITICAL("notification.dispatch.critical"),
    HIGH("notification.dispatch.high"),
    NORMAL("notification.dispatch.normal"),
    LOW("notification.dispatch.bulk");

    private final String dispatchTopic;

    NotificationPriority(String dispatchTopic) {
        this.dispatchTopic = dispatchTopic;
    }

    public String dispatchTopic() {
        return dispatchTopic;
    }

    /** CRITICAL and HIGH are delivered even during a user's quiet hours. */
    public boolean bypassesQuietHours() {
        return this == CRITICAL || this == HIGH;
    }
}
