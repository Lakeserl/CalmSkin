package com.lakeserl.notification_service.dto.response;

import java.time.LocalDateTime;

/** A notification template as shown to admins. */
public record TemplateResponse(
        Long id,
        String code,
        String channel,
        String locale,
        String subject,
        String body,
        String variables,
        boolean active,
        int version,
        LocalDateTime updatedAt
) {
}
