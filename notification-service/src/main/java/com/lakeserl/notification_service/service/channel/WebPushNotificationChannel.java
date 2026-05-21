package com.lakeserl.notification_service.service.channel;

import java.nio.charset.StandardCharsets;
import java.security.Security;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lakeserl.notification_service.config.properties.WebPushProperties;
import com.lakeserl.notification_service.entity.Notification;
import com.lakeserl.notification_service.entity.WebPushSubscription;
import com.lakeserl.notification_service.enums.NotificationChannel;
import com.lakeserl.notification_service.enums.NotificationPriority;
import com.lakeserl.notification_service.event.payload.RecipientContext;
import com.lakeserl.notification_service.repository.WebPushSubscriptionRepository;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.martijndwars.webpush.PushService;

/**
 * Sends W3C Web Push messages (VAPID) to every active browser subscription of
 * the user. Endpoints rejected with 404/410 are deactivated. The channel
 * disables itself when no VAPID keys are configured.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebPushNotificationChannel implements NotificationChannelHandler {

    private final WebPushSubscriptionRepository subscriptionRepository;
    private final WebPushProperties webPushProps;
    private final ObjectMapper objectMapper;

    private PushService pushService;
    private boolean enabled;

    @PostConstruct
    void init() {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
        if (!webPushProps.isConfigured()) {
            log.warn("VAPID keys not configured - Web Push channel is disabled");
            return;
        }
        try {
            pushService = new PushService(
                    webPushProps.vapidPublicKey(),
                    webPushProps.vapidPrivateKey(),
                    webPushProps.subject());
            enabled = true;
            log.info("Web Push channel initialised");
        } catch (Exception ex) {
            log.error("Failed to initialise Web Push channel - it will be disabled", ex);
        }
    }

    /** Whether VAPID keys were supplied and the push service is usable. */
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.WEB_PUSH;
    }

    @Override
    public void deliver(Notification notification, RecipientContext recipient) throws Exception {
        if (!enabled) {
            throw new IllegalStateException("Web Push channel is not configured");
        }

        List<WebPushSubscription> subscriptions =
                subscriptionRepository.findByUserIdAndIsActiveTrue(notification.getUserId());
        if (subscriptions.isEmpty()) {
            log.debug("No active push subscriptions for user={}", notification.getUserId());
            return;
        }

        byte[] payload = buildPayload(notification);
        int delivered = 0;
        boolean retryableFailure = false;

        for (WebPushSubscription sub : subscriptions) {
            try {
                nl.martijndwars.webpush.Notification push = new nl.martijndwars.webpush.Notification(
                        sub.getEndpoint(), sub.getP256dh(), sub.getAuth(), payload);
                int status = pushService.send(push).getStatusLine().getStatusCode();
                if (status >= 200 && status < 300) {
                    sub.setLastUsedAt(LocalDateTime.now());
                    subscriptionRepository.save(sub);
                    delivered++;
                } else if (status == 404 || status == 410) {
                    sub.setIsActive(false);
                    subscriptionRepository.save(sub);
                    log.info("Deactivated expired push endpoint for user={}", notification.getUserId());
                } else {
                    retryableFailure = true;
                    log.warn("Push relay returned status={} for user={}", status, notification.getUserId());
                }
            } catch (Exception ex) {
                retryableFailure = true;
                log.warn("Push send failed for user={}: {}", notification.getUserId(), ex.getMessage());
            }
        }

        if (delivered == 0 && retryableFailure) {
            throw new IllegalStateException("All push deliveries failed for user " + notification.getUserId());
        }
    }

    private byte[] buildPayload(Notification notification) throws Exception {
        Map<String, Object> data = new HashMap<>();
        data.put("referenceId", notification.getReferenceId());
        data.put("referenceType", notification.getReferenceType());

        Map<String, Object> payload = new HashMap<>();
        payload.put("title", notification.getSubject() != null ? notification.getSubject() : "CalmSKIN");
        payload.put("body", notification.getBody());
        payload.put("icon", "/icons/notification-icon.png");
        payload.put("badge", "/icons/badge.png");
        payload.put("tag", notification.getReferenceType() + "-" + notification.getReferenceId());
        payload.put("requireInteraction", notification.getPriority() == NotificationPriority.CRITICAL);
        payload.put("data", data);

        return objectMapper.writeValueAsString(payload).getBytes(StandardCharsets.UTF_8);
    }
}
