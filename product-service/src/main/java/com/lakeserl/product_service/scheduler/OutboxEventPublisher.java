package com.lakeserl.product_service.scheduler;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.lakeserl.product_service.entity.OutboxEvent;
import com.lakeserl.product_service.enums.OutboxStatus;
import com.lakeserl.product_service.repository.OutboxRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxEventPublisher {

    private static final int BATCH_SIZE = 100;
    private static final int MAX_RETRIES = 3;
    private static final long SEND_TIMEOUT_SECONDS = 5;

    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Scheduled(fixedDelay = 1000)
    public void publishPendingEvents() {
        List<OutboxEvent> events = outboxRepository
                .findByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING, PageRequest.of(0, BATCH_SIZE));
        for (OutboxEvent event : events) {
            publishEvent(event);
        }
    }

    private void publishEvent(OutboxEvent event) {
        try {
            kafkaTemplate.send(event.getEventType(), event.getAggregateId(), event.getPayload())
                    .get(SEND_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            event.setStatus(OutboxStatus.SENT);
            event.setProcessedAt(LocalDateTime.now());
            event.setErrorMessage(null);
            outboxRepository.save(event);
        } catch (Exception ex) {
            int retries = (event.getRetryCount() == null ? 0 : event.getRetryCount()) + 1;
            event.setRetryCount(retries);
            event.setErrorMessage(ex.getMessage());
            event.setProcessedAt(null);
            event.setStatus(retries >= MAX_RETRIES ? OutboxStatus.FAILED : OutboxStatus.PENDING);
            log.error("Outbox publish failed (attempt {}) for id={} topic={}: {}",
                    retries, event.getId(), event.getEventType(), ex.getMessage());
            outboxRepository.save(event);
        }
    }
}
