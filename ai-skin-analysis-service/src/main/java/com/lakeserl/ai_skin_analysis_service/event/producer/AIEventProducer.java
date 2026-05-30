package com.lakeserl.ai_skin_analysis_service.event.producer;

import com.lakeserl.ai_skin_analysis_service.event.payload.SkinAnalysisCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AIEventProducer {

    private static final String TOPIC_SKIN_ANALYSIS_COMPLETED = "ai.skin-analysis-completed";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishSkinAnalysisCompleted(SkinAnalysisCompletedEvent event) {
        kafkaTemplate.send(TOPIC_SKIN_ANALYSIS_COMPLETED, event.getSessionId(), event)
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
