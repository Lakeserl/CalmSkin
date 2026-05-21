package com.lakeserl.notification_service.service;

import java.util.UUID;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.lakeserl.notification_service.enums.NotificationChannel;
import com.lakeserl.notification_service.repository.NotificationRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Maintains the unread in-app badge count in Redis so the badge endpoint never
 * touches Postgres. On a cache miss the count is recomputed from the database.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UnreadCountService {

    private static final String PREFIX = "notif:unread:";

    private final StringRedisTemplate redis;
    private final NotificationRepository notificationRepository;

    public long get(UUID userId) {
        String value = redis.opsForValue().get(key(userId));
        if (value != null) {
            try {
                return Long.parseLong(value);
            } catch (NumberFormatException ex) {
                log.warn("Corrupt unread counter for user={}, recomputing", userId);
            }
        }
        return recompute(userId);
    }

    public void increment(UUID userId) {
        Long value = redis.opsForValue().increment(key(userId));
        // If the key was absent, increment() seeds it at 1 which under-counts;
        // recompute so the badge stays accurate.
        if (value != null && value == 1L) {
            recompute(userId);
        }
    }

    public void decrement(UUID userId) {
        Long value = redis.opsForValue().decrement(key(userId));
        if (value == null || value < 0L) {
            recompute(userId);
        }
    }

    public void reset(UUID userId) {
        redis.opsForValue().set(key(userId), "0");
    }

    /** Reloads the counter from Postgres and returns the fresh value. */
    public long recompute(UUID userId) {
        long count = notificationRepository
                .countByUserIdAndChannelAndReadAtIsNull(userId, NotificationChannel.IN_APP);
        redis.opsForValue().set(key(userId), Long.toString(count));
        return count;
    }

    private String key(UUID userId) {
        return PREFIX + userId;
    }
}
