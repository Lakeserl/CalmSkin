package com.lakeserl.product_service.service;

import com.lakeserl.product_service.dto.event.ProductUpdatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    // Master Topic List §8: product-service publishes exactly one topic.
    private static final String TOPIC_PRODUCT_STATUS_CHANGED = "product.status-changed";

    public void publishProductStatusChanged(ProductUpdatedEvent event) {
        try {
            kafkaTemplate.send(TOPIC_PRODUCT_STATUS_CHANGED,
                    String.valueOf(event.getProductId()), event);
        } catch (Exception e) {
            log.error("Failed to publish product.status-changed for product: {}", event.getProductId(), e);
        }
    }
}
