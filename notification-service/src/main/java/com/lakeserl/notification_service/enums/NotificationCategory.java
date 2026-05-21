package com.lakeserl.notification_service.enums;

/**
 * Opt-in category of a notification. Each value maps to a boolean column on
 * {@code notification_preferences}. SECURITY_ALERTS cannot be disabled.
 */
public enum NotificationCategory {
    ORDER_UPDATES,
    PROMOTIONS,
    REVIEWS,
    STOCK_ALERTS,
    SECURITY_ALERTS
}
