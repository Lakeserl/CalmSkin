package com.lakeserl.review_service.event.consumer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lakeserl.review_service.entity.ProcessedKafkaEvent;
import com.lakeserl.review_service.entity.ReviewEligibility;
import com.lakeserl.review_service.repository.ProcessedKafkaEventRepository;
import com.lakeserl.review_service.repository.ReviewEligibilityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventConsumer {

    private final ReviewEligibilityRepository eligibilityRepository;
    private final ProcessedKafkaEventRepository processedEventRepository;
    private final ObjectMapper objectMapper;

    /**
     * Listens to order.completed events and creates review eligibility records
     * for each delivered order item. Idempotent via ProcessedKafkaEvent.
     */
    @KafkaListener(topics = "order.completed", groupId = "review-service")
    @Transactional
    public void handleOrderCompleted(ConsumerRecord<String, String> record, Acknowledgment ack) {
        String eventId = "order.completed:" + record.partition() + ":" + record.offset();

        try {
            if (processedEventRepository.existsById(eventId)) {
                log.debug("Skipping already processed event {}", eventId);
                ack.acknowledge();
                return;
            }

            Map<String, Object> payload = objectMapper.readValue(record.value(), new TypeReference<>() {});
            String orderId = (String) payload.get("orderId");
            UUID userId = UUID.fromString(payload.get("userId").toString());

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> items = (List<Map<String, Object>>) payload.get("items");
            if (items != null) {
                for (Map<String, Object> item : items) {
                    Long orderItemId = Long.parseLong(item.get("orderItemId").toString());
                    Long productId = Long.parseLong(item.get("productId").toString());

                    if (!eligibilityRepository.findByUserIdAndOrderItemId(userId, orderItemId).isPresent()) {
                        ReviewEligibility eligibility = ReviewEligibility.builder()
                                .userId(userId)
                                .orderItemId(orderItemId)
                                .productId(productId)
                                .orderCompletedAt(LocalDateTime.now())
                                .build();
                        eligibilityRepository.save(eligibility);
                    }
                }
            }

            processedEventRepository.save(ProcessedKafkaEvent.builder()
                    .eventId(eventId)
                    .eventType("order.completed")
                    .build());

            log.info("Processed order.completed for orderId={} userId={}", orderId, userId);
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Failed to process order.completed event {}: {}", eventId, e.getMessage(), e);
            // Do not ack — let DLQ handle after retries
        }
    }
}

