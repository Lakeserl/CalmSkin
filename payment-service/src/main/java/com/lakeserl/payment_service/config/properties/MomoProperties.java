package com.lakeserl.payment_service.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Momo sandbox gateway configuration. Bound from app.momo.* in application.yaml.
 * Secrets (accessKey, secretKey) come from environment variables.
 */
@ConfigurationProperties(prefix = "app.momo")
public record MomoProperties(
        String partnerCode,
        String accessKey,
        String secretKey,
        String endpoint,
        String redirectUrl,
        String ipnUrl,
        String requestType,
        String lang) {
}
