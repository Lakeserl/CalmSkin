package com.lakeserl.order_service.event.producer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lakeserl.order_service.entity.OutboxEvent;
import com.lakeserl.order_service.enums.OutboxStatus;
import com.lakeserl.order_service.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventProducer {

    private static final String AGGREGATE_TYPE = "Order";

    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public void publish(String eventType, String aggregateId, Object payload) {
        try {
            String body = objectMapper.writeValueAsString(payload);
            OutboxEvent event = OutboxEvent.builder()
                    .aggregateType(AGGREGATE_TYPE)
                    .aggregateId(aggregateId)
                    .eventType(eventType)
                    .payload(body)
                    .status(OutboxStatus.PENDING)
                    .retryCount(0)
                    .build();
            outboxRepository.save(event);
            log.info("Saved outbox event: type={}, id={}", eventType, aggregateId);
        } catch (JsonProcessingException ex) {
            log.error("Failed to serialize outbox event payload", ex);
            throw new IllegalStateException("Failed to publish outbox event", ex);
        }
    }
}
