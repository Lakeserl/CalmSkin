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
 * Consumes payment.* events. These carry only an orderId, so the owning userId
 * is resolved from the order-to-user map seeded by {@link OrderEventConsumer}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventConsumer {

    private static final List<NotificationChannel> ALL_CHANNELS = List.of(
            NotificationChannel.EMAIL, NotificationChannel.WEB_PUSH, NotificationChannel.IN_APP);

    private final ObjectMapper objectMapper;
    private final NotificationCommandProducer commandProducer;
    private final OrderUserMapService orderUserMapService;

    @KafkaListener(topics = {"payment.completed", "payment.failed", "payment.refunded"},
            groupId = "notification-service")
    public void handle(ConsumerRecord<String, String> record, Acknowledgment ack) {
        try {
            JsonNode node = objectMapper.readTree(record.value());
            String orderId = EventJson.text(node, "orderId");
            if (orderId == null) {
                log.warn("Skipping {} event without orderId", record.topic());
                ack.acknowledge();
                return;
            }
            Optional<UUID> userId = orderUserMapService.findUserId(orderId);
            if (userId.isEmpty()) {
                log.warn("No user mapping for orderId={}, cannot send {} notification",
                        orderId, record.topic());
                ack.acknowledge();
                return;
            }
            String amount = EventJson.textOr(node, "", "amount");

            switch (record.topic()) {
                case "payment.completed" -> commandProducer.publish(NotificationCommand.builder()
                        .dedupKey("payment.completed:" + orderId)
                        .userId(userId.get())
                        .category(NotificationCategory.ORDER_UPDATES)
                        .priority(NotificationPriority.HIGH)
                        .templateCode("PAYMENT_COMPLETED")
                        .channels(ALL_CHANNELS)
                        .title("Payment successful")
                        .body("Your payment of " + amount + " VND was successful.")
                        .variables(Map.of("amount", amount))
                        .referenceType("ORDER")
                        .referenceId(orderId)
                        .build());
                case "payment.failed" -> commandProducer.publish(NotificationCommand.builder()
                        .dedupKey("payment.failed:" + orderId)
                        .userId(userId.get())
                        .category(NotificationCategory.ORDER_UPDATES)
                        .priority(NotificationPriority.HIGH)
                        .templateCode("PAYMENT_FAILED")
                        .channels(ALL_CHANNELS)
                        .title("Payment failed")
                        .body("Your payment could not be completed. Please try again.")
                        .referenceType("ORDER")
                        .referenceId(orderId)
                        .build());
                case "payment.refunded" -> commandProducer.publish(NotificationCommand.builder()
                        .dedupKey("payment.refunded:" + orderId)
                        .userId(userId.get())
                        .category(NotificationCategory.ORDER_UPDATES)
                        .priority(NotificationPriority.NORMAL)
                        .templateCode("PAYMENT_REFUNDED")
                        .channels(ALL_CHANNELS)
                        .title("Refund processed")
                        .body("Your refund of " + amount + " VND has been processed.")
                        .variables(Map.of("amount", amount))
                        .referenceType("ORDER")
                        .referenceId(orderId)
                        .build());
                default -> log.warn("Unhandled payment topic {}", record.topic());
            }
            ack.acknowledge();
        } catch (Exception ex) {
            log.error("Failed to handle payment event from topic={}", record.topic(), ex);
            throw new IllegalStateException("Payment event processing failed", ex);
        }
    }
}
