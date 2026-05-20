package com.lakeserl.product_service.service;

import java.util.List;
import java.util.Map;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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

    @KafkaListener(topics = "order.completed", groupId = "product-service")
    @Transactional
    public void onOrderCompleted(Map<String, Object> event) {
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
        log.info("Applied sold-count update for order {}", event.get("orderId"));
    }
}
