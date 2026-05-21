package com.lakeserl.notification_service.event.consumer;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

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
import com.lakeserl.notification_service.service.OrderUserMapService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Consumes shipping.updated events and notifies the order owner of the new
 * tracking status.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ShippingEventConsumer {

    private final ObjectMapper objectMapper;
    private final NotificationCommandProducer commandProducer;
    private final OrderUserMapService orderUserMapService;

    @KafkaListener(topics = "shipping.updated", groupId = "notification-service")
    public void handle(ConsumerRecord<String, String> record, Acknowledgment ack) {
        try {
            JsonNode node = objectMapper.readTree(record.value());
            String orderId = EventJson.text(node, "orderId");
            if (orderId == null) {
                log.warn("Skipping shipping.updated event without orderId");
                ack.acknowledge();
                return;
            }
            Optional<UUID> userId = orderUserMapService.findUserId(orderId);
            if (userId.isEmpty()) {
                log.warn("No user mapping for orderId={}, cannot send shipping notification", orderId);
                ack.acknowledge();
                return;
            }
            String status = EventJson.textOr(node, "UPDATED", "status", "shippingStatus");
            String tracking = EventJson.textOr(node, "", "trackingNumber");

            commandProducer.publish(NotificationCommand.builder()
                    .dedupKey("shipping.updated:" + orderId + ":" + status)
                    .userId(userId.get())
                    .category(NotificationCategory.ORDER_UPDATES)
                    .priority(NotificationPriority.NORMAL)
                    .templateCode("SHIPPING_UPDATED")
                    .channels(List.of(NotificationChannel.EMAIL,
                            NotificationChannel.WEB_PUSH, NotificationChannel.IN_APP))
                    .title("Shipping update")
                    .body("Your order shipping status is now: " + status)
                    .variables(Map.of("status", status, "trackingNumber", tracking))
                    .referenceType("ORDER")
                    .referenceId(orderId)
                    .build());
            ack.acknowledge();
        } catch (Exception ex) {
            log.error("Failed to handle shipping event", ex);
            throw new IllegalStateException("Shipping event processing failed", ex);
        }
    }
}
