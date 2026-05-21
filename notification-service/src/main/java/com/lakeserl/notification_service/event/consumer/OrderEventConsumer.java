package com.lakeserl.notification_service.event.consumer;

import java.util.List;
import java.util.Map;
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
 * Consumes order.* events. Every order event seeds the orderId-to-userId map so
 * later payment/shipping events (which carry no userId) can be addressed.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventConsumer {

    private static final List<NotificationChannel> ALL_CHANNELS = List.of(
            NotificationChannel.EMAIL, NotificationChannel.WEB_PUSH, NotificationChannel.IN_APP);

    private final ObjectMapper objectMapper;
    private final NotificationCommandProducer commandProducer;
    private final OrderUserMapService orderUserMapService;

    @KafkaListener(topics = {"order.created", "order.confirmed", "order.completed", "order.cancelled"},
            groupId = "notification-service")
    public void handle(ConsumerRecord<String, String> record, Acknowledgment ack) {
        try {
            JsonNode node = objectMapper.readTree(record.value());
            String orderId = EventJson.text(node, "orderId");
            UUID userId = EventJson.uuid(node, "userId");
            if (orderId == null || userId == null) {
                log.warn("Skipping {} event missing orderId/userId", record.topic());
                ack.acknowledge();
                return;
            }
            orderUserMapService.put(orderId, userId);
            String orderNumber = EventJson.textOr(node, orderId, "orderNumber");

            switch (record.topic()) {
                case "order.created" -> commandProducer.publish(NotificationCommand.builder()
                        .dedupKey("order.created:" + orderId)
                        .userId(userId)
                        .category(NotificationCategory.ORDER_UPDATES)
                        .priority(NotificationPriority.NORMAL)
                        .templateCode("ORDER_CREATED")
                        .channels(List.of(NotificationChannel.IN_APP))
                        .title("Order placed")
                        .body("Your order " + orderNumber + " is awaiting payment.")
                        .variables(Map.of("orderNumber", orderNumber))
                        .referenceType("ORDER")
                        .referenceId(orderId)
                        .build());
                case "order.confirmed" -> commandProducer.publish(NotificationCommand.builder()
                        .dedupKey("order.confirmed:" + orderId)
                        .userId(userId)
                        .category(NotificationCategory.ORDER_UPDATES)
                        .priority(NotificationPriority.HIGH)
                        .templateCode("ORDER_CONFIRMED")
                        .channels(ALL_CHANNELS)
                        .title("Order confirmed")
                        .body("Your order " + orderNumber + " has been confirmed.")
                        .variables(Map.of("orderNumber", orderNumber))
                        .referenceType("ORDER")
                        .referenceId(orderId)
                        .build());
                case "order.completed" -> commandProducer.publish(NotificationCommand.builder()
                        .dedupKey("order.completed:" + orderId)
                        .userId(userId)
                        .category(NotificationCategory.ORDER_UPDATES)
                        .priority(NotificationPriority.NORMAL)
                        .templateCode("ORDER_COMPLETED")
                        .channels(ALL_CHANNELS)
                        .title("Order delivered")
                        .body("Your order " + orderNumber + " has been delivered. We would love your review.")
                        .variables(Map.of("orderNumber", orderNumber))
                        .referenceType("ORDER")
                        .referenceId(orderId)
                        .build());
                case "order.cancelled" -> {
                    String reason = EventJson.textOr(node, "", "reason");
                    commandProducer.publish(NotificationCommand.builder()
                            .dedupKey("order.cancelled:" + orderId)
                            .userId(userId)
                            .category(NotificationCategory.ORDER_UPDATES)
                            .priority(NotificationPriority.HIGH)
                            .templateCode("ORDER_CANCELLED")
                            .channels(ALL_CHANNELS)
                            .title("Order cancelled")
                            .body("Your order " + orderNumber + " was cancelled.")
                            .variables(Map.of("orderNumber", orderNumber, "reason", reason))
                            .referenceType("ORDER")
                            .referenceId(orderId)
                            .build());
                }
                default -> log.warn("Unhandled order topic {}", record.topic());
            }
            ack.acknowledge();
        } catch (Exception ex) {
            log.error("Failed to handle order event from topic={}", record.topic(), ex);
            throw new IllegalStateException("Order event processing failed", ex);
        }
    }
}
