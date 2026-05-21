package com.lakeserl.notification_service.dto.response;

import java.util.UUID;

/** Current notification preferences for a user. */
public record PreferenceResponse(
        UUID userId,
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
}
