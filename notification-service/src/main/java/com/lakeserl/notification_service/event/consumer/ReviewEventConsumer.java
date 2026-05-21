package com.lakeserl.notification_service.event.consumer;

import java.util.List;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lakeserl.notification_service.enums.NotificationCategory;
import com.lakeserl.notification_service.enums.NotificationChannel;
import com.lakeserl.notification_service.enums.NotificationPriority;
import com.lakeserl.notification_service.event.payload.NotificationCommand;
import com.lakeserl.notification_service.event.producer.NotificationCommandProducer;
import com.lakeserl.notification_service.support.NotificationConstants;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Consumes review.created events and posts a staff-facing in-app alert so a new
 * product review can be moderated.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReviewEventConsumer {

    private final ObjectMapper objectMapper;
    private final NotificationCommandProducer commandProducer;

    @KafkaListener(topics = "review.created", groupId = "notification-service")
    public void handle(ConsumerRecord<String, String> record, Acknowledgment ack) {
        try {
            JsonNode node = objectMapper.readTree(record.value());
            String reviewId = EventJson.textOr(node, null, "reviewId", "id");
            if (reviewId == null) {
                log.warn("Skipping review.created event without reviewId");
                ack.acknowledge();
                return;
            }
            String productId = EventJson.textOr(node, "", "productId");

            commandProducer.publish(NotificationCommand.builder()
                    .dedupKey("review.created:" + reviewId)
                    .userId(NotificationConstants.ADMIN_FEED_USER_ID)
                    .category(NotificationCategory.REVIEWS)
                    .priority(NotificationPriority.NORMAL)
                    .templateCode("REVIEW_CREATED")
                    .channels(List.of(NotificationChannel.IN_APP))
                    .title("New product review")
                    .body("A new review was posted for product " + productId + ".")
                    .referenceType("REVIEW")
                    .referenceId(reviewId)
                    .build());
            ack.acknowledge();
        } catch (Exception ex) {
            log.error("Failed to handle review event", ex);
            throw new IllegalStateException("Review event processing failed", ex);
        }
    }
}
