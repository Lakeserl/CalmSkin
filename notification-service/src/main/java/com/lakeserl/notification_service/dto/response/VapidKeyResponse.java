package com.lakeserl.notification_service.dto.response;

/** Public VAPID key the frontend needs for pushManager.subscribe(). */
public record VapidKeyResponse(
        String publicKey
) {
}
