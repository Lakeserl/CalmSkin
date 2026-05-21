package com.lakeserl.notification_service.dto.response;

import java.time.LocalDateTime;

/** A single in-app notification as shown in the user's feed. */
public record NotificationResponse(
        Long id,
        String channel,
        String templateCode,
        String subject,
        String body,
        String referenceId,
        String referenceType,
        String status,
        String priority,
        boolean read,
        LocalDateTime readAt,
        String metadata,
        LocalDateTime createdAt
) {
}
