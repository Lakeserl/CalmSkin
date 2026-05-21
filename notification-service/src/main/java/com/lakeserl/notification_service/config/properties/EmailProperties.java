package com.lakeserl.notification_service.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Email sender settings, bound from {@code app.email.*}. The SMTP host itself
 * is provider-grade and configured under {@code spring.mail.*}.
 */
@ConfigurationProperties(prefix = "app.email")
public record EmailProperties(
        String from,
        String replyTo,
        String baseUrl
) {
}
