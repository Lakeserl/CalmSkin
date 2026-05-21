package com.lakeserl.notification_service.dto.response;

/** Admin dashboard counters for the current day. */
public record NotificationStatsResponse(
        long sentToday,
        long failedToday,
        long emailCount,
        long pushCount,
        long inAppCount
) {
}
