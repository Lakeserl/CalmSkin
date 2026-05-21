package com.lakeserl.notification_service.dto.request;

import jakarta.validation.constraints.NotBlank;

/** Identifies the browser subscription to remove. */
public record WebPushUnsubscribeRequest(
        @NotBlank String endpoint
) {
}
