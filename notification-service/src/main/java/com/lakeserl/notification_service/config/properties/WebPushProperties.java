package com.lakeserl.notification_service.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * VAPID keys for the W3C Web Push protocol, bound from {@code app.web-push.*}.
 * When the keys are blank the push channel disables itself instead of failing.
 */
@ConfigurationProperties(prefix = "app.web-push")
public record WebPushProperties(
        String vapidPublicKey,
        String vapidPrivateKey,
        String subject
) {

    public boolean isConfigured() {
        return vapidPublicKey != null && !vapidPublicKey.isBlank()
                && vapidPrivateKey != null && !vapidPrivateKey.isBlank();
    }
}
