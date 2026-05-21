package com.lakeserl.notification_service.service;

import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lakeserl.notification_service.config.properties.NotificationProperties;
import com.lakeserl.notification_service.dto.request.UpdatePreferenceRequest;
import com.lakeserl.notification_service.entity.NotificationPreference;
import com.lakeserl.notification_service.repository.NotificationPreferenceRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Reads and writes notification preferences. The dispatch path reads through a
 * Redis cache ({@link #resolve}); the REST path reads/writes Postgres and
 * evicts the cache. A user with no row uses the default view (no write on the
 * hot path).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PreferenceService {

    private static final String CACHE_PREFIX = "notif:pref:";
    private static final DateTimeFormatter HHMM = DateTimeFormatter.ofPattern("HH:mm");

    private final NotificationPreferenceRepository preferenceRepository;
    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final NotificationProperties props;

    /** Cache-aside read used by the dispatcher. Never writes to Postgres. */
    public PreferenceView resolve(UUID userId) {
        String cached = redis.opsForValue().get(CACHE_PREFIX + userId);
        if (cached != null) {
            try {
                return objectMapper.readValue(cached, PreferenceView.class);
            } catch (Exception ex) {
                log.warn("Corrupt cached preference for user={}, reloading", userId);
            }
        }
        PreferenceView view = preferenceRepository.findByUserId(userId)
                .map(this::toView)
                .orElseGet(this::defaultView);
        cache(userId, view);
        return view;
    }

    /** REST read: returns the persisted row, creating a default one if absent. */
    @Transactional
    public NotificationPreference getOrCreate(UUID userId) {
        return preferenceRepository.findByUserId(userId).orElseGet(() -> {
            try {
                return preferenceRepository.save(defaultEntity(userId));
            } catch (DataIntegrityViolationException raced) {
                return preferenceRepository.findByUserId(userId).orElseThrow();
            }
        });
    }

    @Transactional
    public NotificationPreference update(UUID userId, UpdatePreferenceRequest req) {
        NotificationPreference pref = getOrCreate(userId);
        if (req.emailEnabled() != null) {
            pref.setEmailEnabled(req.emailEnabled());
        }
        if (req.webPushEnabled() != null) {
            pref.setWebPushEnabled(req.webPushEnabled());
        }
        if (req.inAppEnabled() != null) {
            pref.setInAppEnabled(req.inAppEnabled());
        }
        if (req.orderUpdates() != null) {
            pref.setOrderUpdates(req.orderUpdates());
        }
        if (req.promotions() != null) {
            pref.setPromotions(req.promotions());
        }
        if (req.reviews() != null) {
            pref.setReviews(req.reviews());
        }
        if (req.stockAlerts() != null) {
            pref.setStockAlerts(req.stockAlerts());
        }
        // security_alerts is intentionally not editable: it stays true.
        if (req.quietHoursStart() != null) {
            pref.setQuietHoursStart(parseTime(req.quietHoursStart()));
        }
        if (req.quietHoursEnd() != null) {
            pref.setQuietHoursEnd(parseTime(req.quietHoursEnd()));
        }
        if (req.locale() != null && !req.locale().isBlank()) {
            pref.setLocale(req.locale());
        }
        NotificationPreference saved = preferenceRepository.save(pref);
        redis.delete(CACHE_PREFIX + userId);
        return saved;
    }

    /** Disables one category, used by the email unsubscribe flow. */
    @Transactional
    public void disableCategory(UUID userId, String category) {
        NotificationPreference pref = getOrCreate(userId);
        if (category == null) {
            return;
        }
        switch (category.trim().toUpperCase()) {
            case "PROMOTIONS" -> pref.setPromotions(false);
            case "ORDER_UPDATES" -> pref.setOrderUpdates(false);
            case "REVIEWS" -> pref.setReviews(false);
            case "STOCK_ALERTS" -> pref.setStockAlerts(false);
            default -> { /* SECURITY_ALERTS and unknown values are ignored */ }
        }
        preferenceRepository.save(pref);
        redis.delete(CACHE_PREFIX + userId);
    }

    public PreferenceView toView(NotificationPreference p) {
        return new PreferenceView(
                Boolean.TRUE.equals(p.getEmailEnabled()),
                Boolean.TRUE.equals(p.getWebPushEnabled()),
                Boolean.TRUE.equals(p.getInAppEnabled()),
                Boolean.TRUE.equals(p.getOrderUpdates()),
                Boolean.TRUE.equals(p.getPromotions()),
                Boolean.TRUE.equals(p.getReviews()),
                Boolean.TRUE.equals(p.getStockAlerts()),
                true,
                format(p.getQuietHoursStart()),
                format(p.getQuietHoursEnd()),
                p.getLocale());
    }

    private void cache(UUID userId, PreferenceView view) {
        try {
            redis.opsForValue().set(
                    CACHE_PREFIX + userId,
                    objectMapper.writeValueAsString(view),
                    Duration.ofSeconds(props.cacheTtlSeconds()));
        } catch (Exception ex) {
            log.warn("Failed to cache preference for user={}", userId, ex);
        }
    }

    private NotificationPreference defaultEntity(UUID userId) {
        return NotificationPreference.builder()
                .userId(userId)
                .emailEnabled(true)
                .webPushEnabled(true)
                .inAppEnabled(true)
                .orderUpdates(true)
                .promotions(true)
                .reviews(true)
                .stockAlerts(true)
                .securityAlerts(true)
                .quietHoursStart(parseTime(props.defaultQuietStart()))
                .quietHoursEnd(parseTime(props.defaultQuietEnd()))
                .locale(props.defaultLocale())
                .build();
    }

    private PreferenceView defaultView() {
        return new PreferenceView(true, true, true, true, true, true, true, true,
                props.defaultQuietStart(), props.defaultQuietEnd(), props.defaultLocale());
    }

    private LocalTime parseTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return LocalTime.parse(value.trim(), HHMM);
    }

    private String format(LocalTime time) {
        return time == null ? null : time.format(HHMM);
    }
}
