package com.lakeserl.review_service.event.consumer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lakeserl.review_service.entity.ProcessedKafkaEvent;
import com.lakeserl.review_service.repository.ProcessedKafkaEventRepository;
import com.lakeserl.review_service.service.ReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserEventConsumer {

    private final ReviewService reviewService;
    private final ProcessedKafkaEventRepository processedEventRepository;
    private final ObjectMapper objectMapper;

    /**
     * Listens to user.banned events and hides all reviews authored by the banned user.
     * Idempotent via ProcessedKafkaEvent.
     */
    @KafkaListener(topics = "user.banned", groupId = "review-service")
    @Transactional
    public void handleUserBanned(ConsumerRecord<String, String> record, Acknowledgment ack) {
        String eventId = "user.banned:" + record.partition() + ":" + record.offset();

        try {
            if (processedEventRepository.existsById(eventId)) {
                log.debug("Skipping already processed event {}", eventId);
                ack.acknowledge();
                return;
            }

            Map<String, Object> payload = objectMapper.readValue(record.value(), new TypeReference<>() {});
            UUID userId = UUID.fromString(payload.get("userId").toString());

            reviewService.hideReviewsForBannedUser(userId);

            processedEventRepository.save(ProcessedKafkaEvent.builder()
                    .eventId(eventId)
                    .eventType("user.banned")
                    .build());

            log.info("Processed user.banned for userId={}", userId);
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Failed to process user.banned event {}: {}", eventId, e.getMessage(), e);
            // Do not ack — let DLQ handle after retries
        }
    }
}
