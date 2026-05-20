package com.lakeserl.payment_service.event.producer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lakeserl.payment_service.models.entity.OutboxEvent;
import com.lakeserl.payment_service.models.enums.OutboxStatus;
import com.lakeserl.payment_service.repository.OutboxRepository;

import lombok.RequiredArgsConstructor;

/**
 * Publishes domain events to Kafka using the Outbox pattern.
 * Saves serialization results into the {@code outbox_events} table for async processing.
 */
@Component
@RequiredArgsConstructor
public class PaymentEventProducer {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventProducer.class);
    private static final String AGGREGATE_TYPE = "Payment";

    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    /**
     * Save event to outbox table.
     *
     * @param eventType   e.g. "payment.completed", "payment.failed"
     * @param aggregateId e.g. payment ID or order ID
     * @param payload     event payload object
     */
    public void publish(String eventType, String aggregateId, Object payload) {
        try {
            String body = objectMapper.writeValueAsString(payload);
            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .aggregateType(AGGREGATE_TYPE)
                    .aggregateId(aggregateId)
                    .eventType(eventType)
                    .payload(body)
                    .status(OutboxStatus.PENDING)
                    .retryCount(0)
                    .build();

            outboxRepository.save(outboxEvent);
            log.info("Saved outbox event: type={}, aggregateId={}", eventType, aggregateId);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize outbox event payload", e);
            throw new IllegalStateException("Failed to publish outbox event", e);
        }
    }
}
