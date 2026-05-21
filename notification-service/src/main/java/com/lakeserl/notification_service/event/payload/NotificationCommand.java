package com.lakeserl.notification_service.event.payload;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.lakeserl.notification_service.enums.NotificationCategory;
import com.lakeserl.notification_service.enums.NotificationChannel;
import com.lakeserl.notification_service.enums.NotificationPriority;

import lombok.Builder;

/**
 * Internal message produced by the domain-event consumers and republished onto
 * a priority Kafka topic. The dispatch consumer reads it, applies preferences
 * and fans out to channels.
 *
 * <p>{@code dedupKey} uniquely identifies the logical notification. It is used
 * as the Kafka message key so consumer idempotency also dedups logically
 * identical commands. The recipient email is resolved from the local contact
 * cache at delivery time.
 */
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public record NotificationCommand(
        String dedupKey,
        UUID userId,
        NotificationCategory category,
        NotificationPriority priority,
        String templateCode,
        List<NotificationChannel> channels,
        String title,
        String body,
        String referenceId,
        String referenceType,
        Map<String, String> variables,
        LocalDateTime scheduledAt
) {
}
