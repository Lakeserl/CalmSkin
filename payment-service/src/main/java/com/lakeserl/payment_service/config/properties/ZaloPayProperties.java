package com.lakeserl.payment_service.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * ZaloPay sandbox gateway configuration. Bound from app.zalopay.* in
 * application.yaml. Secrets (key1, key2) come from environment variables.
 */
@ConfigurationProperties(prefix = "app.zalopay")
public record ZaloPayProperties(
        String appId,
        String key1,
        String key2,
        String endpoint,
        String callbackUrl,
        String redirectUrl) {
}
