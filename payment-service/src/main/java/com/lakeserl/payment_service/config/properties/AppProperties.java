package com.lakeserl.payment_service.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * General payment-service configuration bound from app.* in application.yaml.
 */
@ConfigurationProperties(prefix = "app")
public record AppProperties(
        String internalSecret,
        Payment payment) {

    public record Payment(int expiryMinutes) {
    }

    /** Expiry window in minutes, defaulting to 15 when not configured. */
    public int expiryMinutes() {
        return payment != null ? payment.expiryMinutes() : 15;
    }
}
