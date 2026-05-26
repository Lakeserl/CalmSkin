package com.lakeserl.subscription_service.scheduler;

import com.lakeserl.subscription_service.entity.OutboxEvent;
import com.lakeserl.subscription_service.enums.OutboxStatus;
import com.lakeserl.subscription_service.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Polls the {@code outbox_events} table every 5 seconds and publishes
 * any PENDING events to Kafka.
 *
 * <p>Uses a fixed-delay schedule (not cron) so it starts the next poll
 * only after the previous one completes, preventing overlap.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxEventPublisher {

    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void publishPendingEvents() {
        List<OutboxEvent> pending = outboxRepository.findTop100ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING);
        if (pending.isEmpty()) return;

        log.debug("OutboxEventPublisher: publishing {} events", pending.size());
        for (OutboxEvent event : pending) {
            try {
                kafkaTemplate.send(event.getEventType(), event.getAggregateId(), event.getPayload())
                        .whenComplete((result, ex) -> {
                            if (ex != null) {
                                log.error("Failed to publish outbox event id={}: {}", event.getId(), ex.getMessage());
                            }
                        });
                event.setStatus(OutboxStatus.SENT);
                event.setProcessedAt(LocalDateTime.now());
            } catch (Exception ex) {
                log.error("Error processing outbox event id={}: {}", event.getId(), ex.getMessage(), ex);
                event.setStatus(OutboxStatus.FAILED);
                event.setRetryCount(event.getRetryCount() + 1);
                event.setLastError(ex.getMessage());
            }
            outboxRepository.save(event);
        }
    }
}
