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
import com.lakeserl.promotion_service.service.VoucherService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Consumes user.registered and grants the configured signup-bonus voucher.
 * Idempotent both via {@code processed_kafka_events} and because the bonus
 * assignment itself is a no-op when the user already holds it.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserEventConsumer {

    private final ObjectMapper objectMapper;
    private final ProcessedKafkaEventRepository processedRepository;
    private final VoucherService voucherService;

    @KafkaListener(topics = "user.registered", groupId = "promotion-service")
    @Transactional
    public void handle(ConsumerRecord<String, String> record, Acknowledgment ack) {
        String eventId = record.topic() + ":"
                + (record.key() != null ? record.key() : UUID.randomUUID().toString());
        if (processedRepository.existsById(eventId)) {
            ack.acknowledge();
            return;
        }
        try {
            JsonNode node = objectMapper.readTree(record.value());
            UUID userId = parseUuid(node.path("userId").asText(null));
            if (userId == null) {
                log.warn("Skipping user.registered event without a valid userId");
                ack.acknowledge();
                return;
            }
            voucherService.assignSignupBonus(userId);
            processedRepository.save(ProcessedKafkaEvent.builder()
                    .eventId(eventId)
                    .eventType(record.topic())
                    .build());
            ack.acknowledge();
        } catch (Exception ex) {
            log.error("Failed to handle user.registered event", ex);
            throw new IllegalStateException("User event processing failed", ex);
        }
    }

    private static UUID parseUuid(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(value.trim());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
