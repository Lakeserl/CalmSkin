package com.lakeserl.ai_skin_analysis_service.event.producer;

import com.lakeserl.ai_skin_analysis_service.event.payload.SkinAnalysisCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class AIEventProducer {

    public static final String TOPIC_SKIN_ANALYSIS_COMPLETED = "ai.skin-analysis-completed";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Sends the event to Kafka and returns the future so callers (the outbox publisher) can
     * await broker acknowledgement before marking the outbox row published.
     */
    public CompletableFuture<SendResult<String, Object>> publishSkinAnalysisCompleted(SkinAnalysisCompletedEvent event) {
        return kafkaTemplate.send(TOPIC_SKIN_ANALYSIS_COMPLETED, event.getSessionId(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish skin-analysis-completed event for sessionId={}: {}",
                                event.getSessionId(), ex.getMessage());
                    } else {
                        log.debug("Published skin-analysis-completed event for sessionId={}", event.getSessionId());
                    }
                });
    }
}
