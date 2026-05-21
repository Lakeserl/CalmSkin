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
 * Consumes inventory stock-warning events. These are staff-facing, so they are
 * stored as IN_APP notifications on the shared admin feed only.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryEventConsumer {

    private final ObjectMapper objectMapper;
    private final NotificationCommandProducer commandProducer;

    @KafkaListener(topics = {"inventory.low-stock", "inventory.out-of-stock"},
            groupId = "notification-service")
    public void handle(ConsumerRecord<String, String> record, Acknowledgment ack) {
        try {
            JsonNode node = objectMapper.readTree(record.value());
            String productId = EventJson.textOr(node, null, "productId", "variantId");
            if (productId == null) {
                log.warn("Skipping {} event without productId", record.topic());
                ack.acknowledge();
                return;
            }
            String productName = EventJson.textOr(node, productId, "productName", "sku");
            boolean outOfStock = "inventory.out-of-stock".equals(record.topic());

            commandProducer.publish(NotificationCommand.builder()
                    .dedupKey(record.topic() + ":" + productId)
                    .userId(NotificationConstants.ADMIN_FEED_USER_ID)
                    .category(NotificationCategory.STOCK_ALERTS)
                    .priority(outOfStock ? NotificationPriority.HIGH : NotificationPriority.NORMAL)
                    .templateCode(outOfStock ? "INVENTORY_OUT_OF_STOCK" : "INVENTORY_LOW_STOCK")
                    .channels(List.of(NotificationChannel.IN_APP))
                    .title(outOfStock ? "Out of stock" : "Low stock alert")
                    .body("Product " + productName + " (" + productId + ") is "
                            + (outOfStock ? "out of stock." : "running low on stock."))
                    .referenceType("PRODUCT")
                    .referenceId(productId)
                    .build());
            ack.acknowledge();
        } catch (Exception ex) {
            log.error("Failed to handle inventory event from topic={}", record.topic(), ex);
            throw new IllegalStateException("Inventory event processing failed", ex);
        }
    }
}
