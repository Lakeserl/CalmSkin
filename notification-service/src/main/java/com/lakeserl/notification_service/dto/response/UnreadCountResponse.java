package com.lakeserl.notification_service.dto.response;

/** Badge count of unread in-app notifications. */
public record UnreadCountResponse(
        long count
) {
}
