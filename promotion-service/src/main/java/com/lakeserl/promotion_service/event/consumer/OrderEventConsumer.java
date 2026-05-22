package com.lakeserl.promotion_service.event.consumer;

import java.util.UUID;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lakeserl.promotion_service.entity.ProcessedKafkaEvent;
import com.lakeserl.promotion_service.repository.ProcessedKafkaEventRepository;
import com.lakeserl.promotion_service.service.PromotionService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Consumes order.* events: a completed order confirms its locked promotions,
 * a cancelled order releases them. Idempotent via {@code processed_kafka_events}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventConsumer {

    private final ObjectMapper objectMapper;
    private final ProcessedKafkaEventRepository processedRepository;
    private final PromotionService promotionService;

    @KafkaListener(topics = {"order.completed", "order.cancelled"}, groupId = "promotion-service")
    @Transactional
    public void handle(ConsumerRecord<String, String> record, Acknowledgment ack) {
        String eventId = record.topic() + ":"
                + (record.key() != null ? record.key() : UUID.randomUUID().toString());
        if (processedRepository.existsById(eventId)) {
            log.info("Event {} already processed, skipping", eventId);
            ack.acknowledge();
            return;
        }
        try {
            JsonNode node = objectMapper.readTree(record.value());
            String orderId = node.path("orderId").asText(null);
            if (orderId == null || orderId.isBlank()) {
                log.warn("Skipping {} event without orderId", record.topic());
                ack.acknowledge();
                return;
            }
            if ("order.completed".equals(record.topic())) {
                promotionService.confirm(orderId);
            } else {
                promotionService.release(orderId);
            }
            processedRepository.save(ProcessedKafkaEvent.builder()
                    .eventId(eventId)
                    .eventType(record.topic())
                    .build());
            ack.acknowledge();
            log.info("Processed {} for orderId={}", record.topic(), orderId);
        } catch (Exception ex) {
            log.error("Failed to handle order event from topic={}", record.topic(), ex);
            throw new IllegalStateException("Order event processing failed", ex);
        }
    }
}
