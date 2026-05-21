package com.lakeserl.notification_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Browser PushSubscription payload sent by the frontend after
 * {@code pushManager.subscribe()}.
 */
public record WebPushSubscribeRequest(
        @NotBlank String endpoint,
        @NotNull Keys keys,
        String browser,
        String os
) {
    public record Keys(
            @NotBlank String p256dh,
            @NotBlank String auth
    ) {
    }
}
