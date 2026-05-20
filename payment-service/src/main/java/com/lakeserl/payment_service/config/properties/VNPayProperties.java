package com.lakeserl.payment_service.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * VNPay sandbox gateway configuration. Bound from app.vnpay.* in application.yaml.
 * Secrets (tmnCode, hashSecret) come from environment variables.
 */
@ConfigurationProperties(prefix = "app.vnpay")
public record VNPayProperties(
        String tmnCode,
        String hashSecret,
        String paymentUrl,
        String apiUrl,
        String returnUrl,
        String version,
        String command,
        String orderType,
        String locale,
        String currCode) {
}
