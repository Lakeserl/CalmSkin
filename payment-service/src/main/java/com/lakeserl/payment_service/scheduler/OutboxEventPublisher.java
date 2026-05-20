package com.lakeserl.payment_service.scheduler;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.lakeserl.payment_service.models.entity.OutboxEvent;
import com.lakeserl.payment_service.models.enums.OutboxStatus;
import com.lakeserl.payment_service.repository.OutboxRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class OutboxEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxEventPublisher.class);
    private static final int BATCH_SIZE = 100;
    private static final int MAX_RETRIES = 3;

    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Scheduled(fixedDelay = 1000)
    public void publishPendingEvents() {
        List<OutboxEvent> events = outboxRepository.findByStatusOrderByCreatedAtAsc(
                OutboxStatus.PENDING, PageRequest.of(0, BATCH_SIZE)
        );

        if (events.isEmpty()) {
            return;
        }

        log.debug("Found {} pending outbox events to publish", events.size());

        for (OutboxEvent event : events) {
            try {
                kafkaTemplate.send(event.getEventType(), event.getAggregateId(), event.getPayload())
                        .get(5, TimeUnit.SECONDS);

                event.setStatus(OutboxStatus.SENT);
                event.setProcessedAt(LocalDateTime.now());
                event.setErrorMessage(null);
            } catch (InterruptedException | java.util.concurrent.ExecutionException | java.util.concurrent.TimeoutException ex) {
                if (ex instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
                int retries = event.getRetryCount() == null ? 0 : event.getRetryCount();
                retries += 1;
                event.setRetryCount(retries);
                event.setErrorMessage(ex.getMessage());

                if (retries >= MAX_RETRIES) {
                    event.setStatus(OutboxStatus.FAILED);
                    log.error("Outbox publish failed after {} retries for id={}", retries, event.getId(), ex);
                } else {
                    log.warn("Outbox publish failed temporarily, retry count={}/{} for id={}", retries, MAX_RETRIES, event.getId());
                }
            }

            outboxRepository.save(event);
        }
    }
}
