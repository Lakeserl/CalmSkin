package com.lakeserl.notification_service.service;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lakeserl.notification_service.dto.request.WebPushSubscribeRequest;
import com.lakeserl.notification_service.entity.WebPushSubscription;
import com.lakeserl.notification_service.repository.WebPushSubscriptionRepository;

import lombok.RequiredArgsConstructor;

/**
 * Registers and removes browser Web Push subscriptions. Re-subscribing with a
 * known endpoint refreshes its keys and owner rather than creating a duplicate.
 */
@Service
@RequiredArgsConstructor
public class WebPushSubscriptionService {

    private final WebPushSubscriptionRepository subscriptionRepository;

    @Transactional
    public void subscribe(UUID userId, WebPushSubscribeRequest request) {
        WebPushSubscription subscription = subscriptionRepository.findByEndpoint(request.endpoint())
                .orElseGet(() -> WebPushSubscription.builder().endpoint(request.endpoint()).build());
        subscription.setUserId(userId);
        subscription.setP256dh(request.keys().p256dh());
        subscription.setAuth(request.keys().auth());
        subscription.setBrowser(request.browser());
        subscription.setOs(request.os());
        subscription.setIsActive(true);
        subscription.setLastUsedAt(LocalDateTime.now());
        subscriptionRepository.save(subscription);
    }

    @Transactional
    public void unsubscribe(UUID userId, String endpoint) {
        subscriptionRepository.deleteByEndpointAndUserId(endpoint, userId);
    }
}
