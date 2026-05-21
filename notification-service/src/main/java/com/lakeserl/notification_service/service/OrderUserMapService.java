package com.lakeserl.notification_service.service;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.lakeserl.notification_service.config.properties.NotificationProperties;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Maps an orderId to its owning userId. Payment events carry only an orderId,
 * so order events seed this map and payment consumers read it back. Backed by
 * Redis with a long TTL; a miss simply means that order's payment notification
 * is skipped (acceptable degradation).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderUserMapService {

    private static final String PREFIX = "notif:order:";

    private final StringRedisTemplate redis;
    private final NotificationProperties props;

    public void put(String orderId, UUID userId) {
        if (orderId == null || userId == null) {
            return;
        }
        redis.opsForValue().set(
                PREFIX + orderId,
                userId.toString(),
                Duration.ofSeconds(props.orderMapTtlSeconds()));
    }

    public Optional<UUID> findUserId(String orderId) {
        if (orderId == null) {
            return Optional.empty();
        }
        String value = redis.opsForValue().get(PREFIX + orderId);
        if (value == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(UUID.fromString(value));
        } catch (IllegalArgumentException ex) {
            log.warn("Corrupt order-user mapping for orderId={}", orderId);
            return Optional.empty();
        }
    }
}
