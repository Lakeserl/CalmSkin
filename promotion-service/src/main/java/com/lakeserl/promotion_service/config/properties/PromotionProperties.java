package com.lakeserl.promotion_service.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Tunables for the promotion engine, bound from {@code app.promotion.*}.
 */
@ConfigurationProperties(prefix = "app.promotion")
public record PromotionProperties(
        int lockTtlMinutes,
        int voucherClaimRatePerMinute,
        int codeInfoRatePerMinute,
        String signupBonusCode,
        long cacheTtlSeconds,
        long activeListTtlSeconds,
        long flashCacheTtlSeconds
) {
}
