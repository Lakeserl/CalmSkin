package com.lakeserl.notification_service.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Tunables for the notification pipeline, bound from {@code app.notification.*}.
 * Quiet hours are evaluated against a fixed zone offset (Vietnam, UTC+7) because
 * the user entity has no per-user timezone yet.
 */
@ConfigurationProperties(prefix = "app.notification")
public record NotificationProperties(
        String quietZoneOffset,
        String defaultQuietStart,
        String defaultQuietEnd,
        String defaultLocale,
        int maxRetry,
        long dedupTtlSeconds,
        long idempotencyTtlSeconds,
        long cacheTtlSeconds,
        long orderMapTtlSeconds
) {
}
