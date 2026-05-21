package com.lakeserl.notification_service.dto.request;

/** Admin edit of a notification template. Null fields are left unchanged. */
public record UpdateTemplateRequest(
        String subject,
        String body,
        String variables,
        Boolean isActive
) {
}
