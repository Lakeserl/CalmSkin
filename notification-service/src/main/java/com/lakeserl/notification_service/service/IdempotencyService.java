package com.lakeserl.notification_service.service;

import java.time.Duration;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.lakeserl.notification_service.config.properties.NotificationProperties;

import lombok.RequiredArgsConstructor;

/**
 * Redis-backed idempotency. Replaces a Postgres processed-events table so the
 * hot path stays fast.
 *
 * <ul>
 *   <li>Kafka idempotency keys ({@code notif:idem:*}) are written only after a
 *       message is fully processed, so a mid-flight failure is safely retried.</li>
 *   <li>Dispatch dedup keys ({@code notif:dedup:*}) use atomic SETNX to drop a
 *       logically identical notification within a short window.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private static final String IDEM_PREFIX = "notif:idem:";
    private static final String DEDUP_PREFIX = "notif:dedup:";

    private final StringRedisTemplate redis;
    private final NotificationProperties props;

    /** True if this Kafka message was already processed. */
    public boolean isDuplicate(String topic, String messageKey) {
        return Boolean.TRUE.equals(redis.hasKey(IDEM_PREFIX + topic + ":" + messageKey));
    }

    /** Records a Kafka message as processed; call only after success. */
    public void markProcessed(String topic, String messageKey) {
        redis.opsForValue().set(
                IDEM_PREFIX + topic + ":" + messageKey,
                "1",
                Duration.ofSeconds(props.idempotencyTtlSeconds()));
    }

    /**
     * Atomically claims a dispatch dedup key. Returns true if the caller may
     * proceed, false if an identical notification was already dispatched.
     */
    public boolean acceptDispatch(String dedupKey) {
        Boolean claimed = redis.opsForValue().setIfAbsent(
                DEDUP_PREFIX + dedupKey,
                "1",
                Duration.ofSeconds(props.dedupTtlSeconds()));
        return Boolean.TRUE.equals(claimed);
    }
}
