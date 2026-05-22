package com.lakeserl.promotion_service.event.producer;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lakeserl.promotion_service.entity.OutboxEvent;
import com.lakeserl.promotion_service.enums.OutboxStatus;
import com.lakeserl.promotion_service.repository.OutboxRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Publishes domain events with the transactional outbox pattern: the event row
 * is written in the same transaction as the business change and shipped to
 * Kafka later by {@code OutboxEventPublisher}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PromotionEventProducer {

    private static final String AGGREGATE_TYPE = "Promotion";

    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public void publish(String eventType, String aggregateId, Object payload) {
        try {
            OutboxEvent event = OutboxEvent.builder()
                    .aggregateType(AGGREGATE_TYPE)
                    .aggregateId(aggregateId)
                    .eventType(eventType)
                    .payload(objectMapper.writeValueAsString(payload))
                    .status(OutboxStatus.PENDING)
                    .retryCount(0)
                    .build();
            outboxRepository.save(event);
            log.info("Saved outbox event: type={}, aggregateId={}", eventType, aggregateId);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialise outbox event payload", ex);
        }
    }
}
