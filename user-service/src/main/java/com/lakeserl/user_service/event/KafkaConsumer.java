package com.lakeserl.user_service.event;

import java.util.Map;
import java.util.UUID;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lakeserl.user_service.model.entity.ProcessedKafkaEvent;
import com.lakeserl.user_service.repository.ProcessedKafkaEventRepository;
import com.lakeserl.user_service.service.LoyaltyService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaConsumer {

    private final LoyaltyService loyaltyService;
    private final ProcessedKafkaEventRepository processedKafkaEventRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "order.created", groupId = "user-service")
    @Transactional
    public void handleOrderCreated(ConsumerRecord<String, String> record, Acknowledgment ack) {
        String eventId = "order.created:" + record.key();
        if (processedKafkaEventRepository.existsById(eventId)) {
            ack.acknowledge();
            return;
        }
        try {
            Map<String, Object> event = objectMapper.readValue(record.value(), new TypeReference<>() {});
            String userId = (String) event.get("userId");
            Object pointsVal = event.get("pointsUsed");
            if (userId != null && pointsVal != null) {
                int pointsUsed = ((Number) pointsVal).intValue();
                if (pointsUsed > 0) {
                    String orderId = (String) event.get("orderId");
                    loyaltyService.redeemPoints(UUID.fromString(userId), pointsUsed, orderId, "ORDER", "Order placement deduction");
                    log.info("Points deducted user={} points={}", userId, pointsUsed);
                }
            }
            processedKafkaEventRepository.save(ProcessedKafkaEvent.builder()
                    .eventId(eventId).eventType("order.created").build());
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Failed to process order.created key={}", record.key(), e);
            throw new RuntimeException(e);
        }
    }

    @KafkaListener(topics = "order.completed", groupId = "user-service")
    @Transactional
    public void handleOrderCompleted(ConsumerRecord<String, String> record, Acknowledgment ack) {
        String eventId = "order.completed:" + record.key();
        if (processedKafkaEventRepository.existsById(eventId)) {
            ack.acknowledge();
            return;
        }
        try {
            Map<String, Object> event = objectMapper.readValue(record.value(), new TypeReference<>() {});
            String userId = (String) event.get("userId");
            Object pointsVal = event.get("pointsEarned");
            if (userId != null && pointsVal != null) {
                int pointsEarned = ((Number) pointsVal).intValue();
                if (pointsEarned > 0) {
                    String orderId = (String) event.get("orderId");
                    loyaltyService.earnPoints(UUID.fromString(userId), pointsEarned, orderId, "ORDER", "Order completed");
                    log.info("Points earned user={} points={}", userId, pointsEarned);
                }
            }
            processedKafkaEventRepository.save(ProcessedKafkaEvent.builder()
                    .eventId(eventId).eventType("order.completed").build());
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Failed to process order.completed key={}", record.key(), e);
            throw new RuntimeException(e);
        }
    }

    @KafkaListener(topics = "order.cancelled", groupId = "user-service")
    @Transactional
    public void handleOrderCancelled(ConsumerRecord<String, String> record, Acknowledgment ack) {
        String eventId = "order.cancelled:" + record.key();
        if (processedKafkaEventRepository.existsById(eventId)) {
            ack.acknowledge();
            return;
        }
        try {
            Map<String, Object> event = objectMapper.readValue(record.value(), new TypeReference<>() {});
            String userId = (String) event.get("userId");
            Object pointsVal = event.get("pointsUsed");
            if (userId != null && pointsVal != null) {
                int pointsUsed = ((Number) pointsVal).intValue();
                if (pointsUsed > 0) {
                    String orderId = (String) event.get("orderId");
                    loyaltyService.earnPoints(UUID.fromString(userId), pointsUsed, orderId, "ORDER", "Order cancelled refund");
                    log.info("Points refunded user={} points={}", userId, pointsUsed);
                }
            }
            processedKafkaEventRepository.save(ProcessedKafkaEvent.builder()
                    .eventId(eventId).eventType("order.cancelled").build());
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Failed to process order.cancelled key={}", record.key(), e);
            throw new RuntimeException(e);
        }
    }
}
