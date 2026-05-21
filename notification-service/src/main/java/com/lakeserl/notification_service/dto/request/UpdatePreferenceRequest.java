package com.lakeserl.notification_service.dto.request;

/**
 * Full preference update. A null field leaves the current value unchanged.
 * Quiet hours are "HH:mm" strings in Vietnam time; blank disables them.
 * security_alerts cannot be turned off and is ignored if sent as false.
 */
public record UpdatePreferenceRequest(
        Boolean emailEnabled,
        Boolean webPushEnabled,
        Boolean inAppEnabled,
        Boolean orderUpdates,
        Boolean promotions,
        Boolean reviews,
        Boolean stockAlerts,
        String quietHoursStart,
        String quietHoursEnd,
        String locale
) {
}
