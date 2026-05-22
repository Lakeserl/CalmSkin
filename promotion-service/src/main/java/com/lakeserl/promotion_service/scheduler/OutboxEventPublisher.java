package com.lakeserl.promotion_service.scheduler;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.lakeserl.promotion_service.entity.OutboxEvent;
import com.lakeserl.promotion_service.enums.OutboxStatus;
import com.lakeserl.promotion_service.repository.OutboxRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Ships pending transactional-outbox rows to Kafka once per second. The event
 * type is used as the topic; failures are retried a few times before the row
 * is marked FAILED.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxEventPublisher {

    private static final int BATCH_SIZE = 100;
    private static final int MAX_RETRIES = 3;

    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Scheduled(fixedDelay = 1000)
    public void publishPendingEvents() {
        List<OutboxEvent> events = outboxRepository.findByStatusOrderByCreatedAtAsc(
                OutboxStatus.PENDING, PageRequest.of(0, BATCH_SIZE));
        if (events.isEmpty()) {
            return;
        }

        for (OutboxEvent event : events) {
            try {
                kafkaTemplate.send(event.getEventType(), event.getAggregateId(), event.getPayload())
                        .get(5, TimeUnit.SECONDS);
                event.setStatus(OutboxStatus.SENT);
                event.setProcessedAt(LocalDateTime.now());
                event.setErrorMessage(null);
            } catch (InterruptedException | ExecutionException | TimeoutException ex) {
                if (ex instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
                int retries = (event.getRetryCount() == null ? 0 : event.getRetryCount()) + 1;
                event.setRetryCount(retries);
                event.setErrorMessage(ex.getMessage());
                if (retries >= MAX_RETRIES) {
                    event.setStatus(OutboxStatus.FAILED);
                    log.error("Outbox publish failed after {} retries for id={}", retries, event.getId(), ex);
                } else {
                    log.warn("Outbox publish retry {}/{} for id={}", retries, MAX_RETRIES, event.getId());
                }
            }
            outboxRepository.save(event);
        }
    }
}
