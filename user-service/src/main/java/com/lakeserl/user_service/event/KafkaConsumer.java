package com.lakeserl.user_service.event;

import java.util.Map;
import java.util.UUID;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.lakeserl.user_service.service.LoyaltyService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaConsumer {

    private final LoyaltyService loyaltyService;

    @KafkaListener(topics = "order.completed", groupId = "user-service")
    public void handleOrderCompleted(Map<String, Object> event) {
        String userId = (String) event.get("userId");
        int points = ((Number) event.get("points")).intValue();
        String orderId = (String) event.get("orderId");

        loyaltyService.earnPoints(UUID.fromString(userId), points, orderId, "ORDER", "Order completed");
        log.info("Points earned for user={} points={}", userId, points);
    }

    @KafkaListener(topics = "order.cancelled", groupId = "user-service")
    public void handleOrderCancelled(Map<String, Object> event) {
        String userId = (String) event.get("userId");
        int points = ((Number) event.get("points")).intValue();
        String orderId = (String) event.get("orderId");

        loyaltyService.redeemPoints(UUID.fromString(userId), points, orderId, "ORDER", "Order cancelled refund");
        log.info("Points refunded for user={} points={}", userId, points);
    }
}
