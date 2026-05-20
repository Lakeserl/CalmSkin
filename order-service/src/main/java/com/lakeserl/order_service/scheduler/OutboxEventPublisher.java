package com.lakeserl.order_service.scheduler;

import com.lakeserl.order_service.entity.OutboxEvent;
import com.lakeserl.order_service.enums.OutboxStatus;
import com.lakeserl.order_service.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxEventPublisher {

    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Scheduled(fixedDelay = 1000)
    public void publishPendingEvents() {
        List<OutboxEvent> events = outboxRepository.findByStatusOrderByCreatedAtAsc(
                OutboxStatus.PENDING, PageRequest.of(0, 100));

        if (events.isEmpty()) {
            return;
        }

        log.debug("Found {} pending outbox events to publish", events.size());
        for (OutboxEvent event : events) {
            publishEvent(event);
        }
    }

    private void publishEvent(OutboxEvent event) {
        String topic = resolveTopic(event.getEventType());
        try {
            log.info("Publishing outbox event id={} to topic={} with key={}", 
                    event.getId(), topic, event.getAggregateId());
            
            // Send synchronously with 5s timeout to ensure ordering / reliability
            kafkaTemplate.send(topic, event.getAggregateId(), event.getPayload())
                    .get(5, TimeUnit.SECONDS);

            event.setStatus(OutboxStatus.SENT);
            event.setProcessedAt(LocalDateTime.now());
            outboxRepository.save(event);

        } catch (Exception ex) {
            log.error("Failed to publish outbox event id={} due to: {}", event.getId(), ex.getMessage());
            event.setRetryCount(event.getRetryCount() + 1);
            event.setErrorMessage(ex.getMessage());
            
            if (event.getRetryCount() >= 3) {
                event.setStatus(OutboxStatus.FAILED);
                log.error("Outbox event id={} marked as FAILED after max retries", event.getId());
            }
            outboxRepository.save(event);
        }
    }

    private String resolveTopic(String eventType) {
        // Automatically route event types (e.g. order.created -> topic order.created)
        return eventType;
    }
}
