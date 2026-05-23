package com.lakeserl.product_service.service;

import java.util.List;
import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lakeserl.product_service.entity.ProcessedKafkaEvent;
import com.lakeserl.product_service.repository.ProcessedKafkaEventRepository;
import com.lakeserl.product_service.repository.ProductRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Consumes order events that affect the product catalogue.
 *
 * Master Topic List §8: order.completed is consumed by product-service to
 * accumulate sold_count. The event carries the order items snapshot
 * (productId + quantity) published by order-service.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProductEventConsumer {

    private final ProductRepository productRepository;
    private final ProcessedKafkaEventRepository processedKafkaEventRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "order.completed", groupId = "product-service")
    @Transactional
    public void onOrderCompleted(ConsumerRecord<String, String> record) {
        String eventId = record.key() != null
                ? record.topic() + ":" + record.key()
                : record.topic() + ":" + record.partition() + ":" + record.offset();
        if (processedKafkaEventRepository.existsById(eventId)) {
            log.debug("Skipping duplicate order.completed event: {}", eventId);
            return;
        }

        Map<String, Object> event;
        try {
            event = objectMapper.readValue(record.value(), Map.class);
        } catch (Exception e) {
            log.error("Failed to parse order.completed event payload", e);
            return;
        }

        Object rawItems = event.get("items");
        if (!(rawItems instanceof List<?> items)) {
            log.warn("order.completed without items, skipping sold-count update: {}", event.get("orderId"));
            return;
        }

        for (Object rawItem : items) {
            if (!(rawItem instanceof Map<?, ?> item)) {
                continue;
            }
            Object productId = item.get("productId");
            Object quantity = item.get("quantity");
            if (productId == null || quantity == null) {
                continue;
            }
            long productIdValue = ((Number) productId).longValue();
            long quantityValue = ((Number) quantity).longValue();

            int updated = productRepository.incrementSoldCount(productIdValue, quantityValue);
            if (updated == 0) {
                log.warn("order.completed referenced unknown product id={}", productIdValue);
            }
        }

        processedKafkaEventRepository.save(
                new ProcessedKafkaEvent(eventId, "order.completed", null));
        log.info("Applied sold-count update for order {}", event.get("orderId"));
    }
}
