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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

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
        List<OutboxEvent> events = outboxRepository.findByStatusOrderByCreatedAtAsc(
                OutboxStatus.PENDING, PageRequest.of(0, BATCH_SIZE));

        if (events.isEmpty()) {
            return;
        }

        log.debug("Found {} pending outbox events to publish", events.size());
        for (OutboxEvent event : events) {
            publishEvent(event);
        }
    }

    private void publishEvent(OutboxEvent event) {
        log.info("Publishing outbox event id={} to topic={} key={}",
                event.getId(), event.getEventType(), event.getAggregateId());
        try {
            kafkaTemplate.send(event.getEventType(), event.getAggregateId(), event.getPayload())
                    .get(SEND_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            event.setStatus(OutboxStatus.SENT);
            event.setProcessedAt(LocalDateTime.now());
            event.setErrorMessage(null);
            outboxRepository.save(event);
        } catch (Exception ex) {
            Integer prev = event.getRetryCount();
            int retries = (prev == null ? 0 : prev.intValue()) + 1;
            event.setRetryCount(retries);
            event.setErrorMessage(ex.getMessage());
            event.setStatus(retries >= MAX_RETRIES ? OutboxStatus.FAILED : OutboxStatus.PENDING);
            log.error("Outbox publish failed (attempt {}) for id={}: {}", retries, event.getId(), ex.getMessage());
            outboxRepository.save(event);
        }
    }
}
