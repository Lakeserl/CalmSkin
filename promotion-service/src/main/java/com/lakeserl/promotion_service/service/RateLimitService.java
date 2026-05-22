package com.lakeserl.promotion_service.service;

import java.time.Duration;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

/**
 * Fixed-window per-minute rate limiter backed by Redis. Used to throttle
 * voucher-claim and voucher-code lookups (anti-abuse).
 */
@Service
@RequiredArgsConstructor
public class RateLimitService {

    private final StringRedisTemplate redis;

    /**
     * Records one hit against {@code key} and reports whether the caller is
     * still within {@code limitPerMinute} for the current 60s window.
     */
    public boolean tryAcquire(String key, int limitPerMinute) {
        Long count = redis.opsForValue().increment(key);
        if (count != null && count == 1L) {
            redis.expire(key, Duration.ofSeconds(60));
        }
        return count == null || count <= limitPerMinute;
    }

    public boolean voucherClaimAllowed(String userId, int limitPerMinute) {
        return tryAcquire("ratelimit:voucher-claim:" + userId, limitPerMinute);
    }

    public boolean codeInfoAllowed(String clientIp, int limitPerMinute) {
        return tryAcquire("ratelimit:code-info:" + clientIp, limitPerMinute);
    }
}
