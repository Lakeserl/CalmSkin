package com.lakeserl.ai_skin_analysis_service.event.producer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lakeserl.ai_skin_analysis_service.entity.OutboxEvent;
import com.lakeserl.ai_skin_analysis_service.event.payload.SkinAnalysisCompletedEvent;
import com.lakeserl.ai_skin_analysis_service.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Relays unpublished {@link OutboxEvent} rows to Kafka. Business code persists events to the
 * outbox inside its own transaction; this poller publishes them and marks a row published only
 * after the broker acknowledges, which closes the dual-write/lost-event gap of sending to Kafka
 * directly inside a DB transaction.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxEventPublisher {

    private final OutboxRepository outboxRepository;
    private final AIEventProducer eventProducer;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelayString = "${app.outbox.poll-interval-ms:5000}")
    @Transactional
    public void publishPending() {
        List<OutboxEvent> pending = outboxRepository.findByPublishedFalseOrderByCreatedAtAsc();
        if (pending.isEmpty()) {
            return;
        }
        for (OutboxEvent event : pending) {
            try {
                relay(event);
                event.setPublished(true);
                outboxRepository.save(event);
            } catch (Exception e) {
                // Leave this row unpublished and stop the cycle to preserve ordering; it is
                // retried on the next tick. At-least-once delivery — consumers are idempotent.
                log.error("Failed to relay outbox event id={} (type={}): {} — will retry",
                        event.getId(), event.getEventType(), e.getMessage());
                break;
            }
        }
    }

    private void relay(OutboxEvent event) throws Exception {
        if (AIEventProducer.TOPIC_SKIN_ANALYSIS_COMPLETED.equals(event.getEventType())) {
            SkinAnalysisCompletedEvent payload =
                    objectMapper.readValue(event.getPayload(), SkinAnalysisCompletedEvent.class);
            eventProducer.publishSkinAnalysisCompleted(payload).get(10, TimeUnit.SECONDS);
        } else {
            throw new IllegalStateException("Unknown outbox event type: " + event.getEventType());
        }
    }
}
