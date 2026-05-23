package com.lakeserl.user_service.event;

import java.util.Map;
import java.util.UUID;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.lakeserl.user_service.service.LoyaltyService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaConsumer {

    private final LoyaltyService loyaltyService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "order.created", groupId = "user-service")
    public void handleOrderCreated(String message) {
        try {
            Map<String, Object> event = objectMapper.readValue(message, new TypeReference<Map<String, Object>>() {});
            String userId = (String) event.get("userId");
            Object pointsVal = event.get("pointsUsed");
            if (userId != null && pointsVal != null) {
                int pointsUsed = ((Number) pointsVal).intValue();
                String orderId = (String) event.get("orderId");
                if (pointsUsed > 0) {
                    loyaltyService.redeemPoints(UUID.fromString(userId), pointsUsed, orderId, "ORDER", "Order placement deduction");
                    log.info("Points deducted for user={} points={}", userId, pointsUsed);
                }
            }
        } catch (Exception e) {
            log.error("Error processing order.created event", e);
        }
    }

    @KafkaListener(topics = "order.completed", groupId = "user-service")
    public void handleOrderCompleted(String message) {
        try {
            Map<String, Object> event = objectMapper.readValue(message, new TypeReference<Map<String, Object>>() {});
            String userId = (String) event.get("userId");
            Object pointsVal = event.get("pointsEarned");
            if (userId != null && pointsVal != null) {
                int pointsEarned = ((Number) pointsVal).intValue();
                String orderId = (String) event.get("orderId");
                if (pointsEarned > 0) {
                    loyaltyService.earnPoints(UUID.fromString(userId), pointsEarned, orderId, "ORDER", "Order completed");
                    log.info("Points earned for user={} points={}", userId, pointsEarned);
                }
            }
        } catch (Exception e) {
            log.error("Error processing order.completed event", e);
        }
    }

    @KafkaListener(topics = "order.cancelled", groupId = "user-service")
    public void handleOrderCancelled(String message) {
        try {
            Map<String, Object> event = objectMapper.readValue(message, new TypeReference<Map<String, Object>>() {});
            String userId = (String) event.get("userId");
            Object pointsVal = event.get("pointsUsed");
            if (userId != null && pointsVal != null) {
                int pointsUsed = ((Number) pointsVal).intValue();
                String orderId = (String) event.get("orderId");
                if (pointsUsed > 0) {
                    loyaltyService.earnPoints(UUID.fromString(userId), pointsUsed, orderId, "ORDER", "Order cancelled refund");
                    log.info("Points refunded for user={} points={}", userId, pointsUsed);
                }
            }
        } catch (Exception e) {
            log.error("Error processing order.cancelled event", e);
        }
    }
}
