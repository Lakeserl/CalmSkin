package com.lakeserl.notification_service.service;

import com.lakeserl.notification_service.enums.NotificationCategory;
import com.lakeserl.notification_service.enums.NotificationChannel;

/**
 * Immutable, cache-friendly snapshot of a user's notification preferences.
 * Quiet hours are "HH:mm" strings (blank = no quiet hours).
 */
public record PreferenceView(
        boolean emailEnabled,
        boolean webPushEnabled,
        boolean inAppEnabled,
        boolean orderUpdates,
        boolean promotions,
        boolean reviews,
        boolean stockAlerts,
        boolean securityAlerts,
        String quietHoursStart,
        String quietHoursEnd,
        String locale
) {

    /** Whether the user opted in to this category. SECURITY_ALERTS is always on. */
    public boolean allows(NotificationCategory category) {
        return switch (category) {
            case ORDER_UPDATES -> orderUpdates;
            case PROMOTIONS -> promotions;
            case REVIEWS -> reviews;
            case STOCK_ALERTS -> stockAlerts;
            case SECURITY_ALERTS -> true;
        };
    }

    /** Whether the user enabled this delivery channel. */
    public boolean allows(NotificationChannel channel) {
        return switch (channel) {
            case EMAIL -> emailEnabled;
            case WEB_PUSH -> webPushEnabled;
            case IN_APP -> inAppEnabled;
        };
    }
}
