package com.lakeserl.product_service.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lakeserl.product_service.dto.event.ProductUpdatedEvent;
import com.lakeserl.product_service.entity.OutboxEvent;
import com.lakeserl.product_service.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Writes Kafka events to outbox_events within the caller's @Transactional boundary.
 * OutboxEventPublisher scheduler delivers them — Invariant §14.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductEventProducer {

    private static final String TOPIC_PRODUCT_STATUS_CHANGED = "product.status-changed";

    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public void publishProductStatusChanged(ProductUpdatedEvent event) {
        try {
            outboxRepository.save(OutboxEvent.builder()
                    .aggregateType("Product")
                    .aggregateId(String.valueOf(event.getProductId()))
                    .eventType(TOPIC_PRODUCT_STATUS_CHANGED)
                    .payload(objectMapper.writeValueAsString(event))
                    .build());
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize outbox payload for product id={}", event.getProductId(), e);
            throw new IllegalStateException("Outbox serialization failed for product.status-changed", e);
        }
    }
}
