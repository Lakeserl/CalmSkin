package com.lakeserl.notification_service.dto.request;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

/**
 * Admin request to send one template to a set of users. A null
 * {@code scheduledAt} means send immediately.
 */
public record BroadcastRequest(
        @NotBlank String templateCode,
        @NotEmpty List<UUID> userIds,
        String title,
        String body,
        LocalDateTime scheduledAt
) {
}
