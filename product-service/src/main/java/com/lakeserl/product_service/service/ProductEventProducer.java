package com.lakeserl.product_service.service;

import com.lakeserl.product_service.dto.event.ProductUpdatedEvent;
import com.lakeserl.product_service.dto.event.ProductViewedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String TOPIC_PRODUCT_VIEWED = "product-viewed";
    private static final String TOPIC_PRODUCT_UPDATED = "product-updated";

    public void publishProductViewed(ProductViewedEvent event) {
        try {
            kafkaTemplate.send(TOPIC_PRODUCT_VIEWED, String.valueOf(event.getProductId()), event);
        } catch (Exception e) {
            log.error("Failed to publish ProductViewedEvent for product: {}", event.getProductId(), e);
        }
    }

    public void publishProductUpdated(ProductUpdatedEvent event) {
        try {
            kafkaTemplate.send(TOPIC_PRODUCT_UPDATED, String.valueOf(event.getProductId()), event);
        } catch (Exception e) {
            log.error("Failed to publish ProductUpdatedEvent for product: {}", event.getProductId(), e);
        }
    }
}
