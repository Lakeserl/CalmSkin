package com.lakeserl.search_service.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lakeserl.search_service.dto.ProductIndexDTO;
import com.lakeserl.search_service.service.SearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductEventConsumer {

    private static final String IDEMPOTENCY_PREFIX = "search:processed:";

    private final SearchService searchService;
    private final ObjectMapper objectMapper;
    private final RedisTemplate<String, String> redisTemplate;

    @KafkaListener(
            topics = {"product.created", "product.updated", "product.status-changed"},
            groupId = "${spring.kafka.consumer.group-id:search-service-group}"
    )
    public void onProductEvent(@Payload String payload,
                               @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                               @Header(KafkaHeaders.OFFSET) long offset,
                               @Header(KafkaHeaders.RECEIVED_PARTITION) int partition) {
        String idempotencyKey = IDEMPOTENCY_PREFIX + topic + ":" + partition + ":" + offset;
        Boolean isNew = redisTemplate.opsForValue().setIfAbsent(idempotencyKey, "1", Duration.ofDays(1));
        if (!Boolean.TRUE.equals(isNew)) {
            log.debug("Duplicate event skipped: key={}", idempotencyKey);
            return;
        }

        try {
            JsonNode node = objectMapper.readTree(payload);
            Long productId = node.path("productId").asLong(-1);
            if (productId < 0) {
                log.warn("product event missing productId, topic={}, payload={}", topic, payload);
                return;
            }

            if ("product.deleted".equals(topic)) {
                searchService.deleteProduct(String.valueOf(productId));
                log.info("Deleted product id={} from index via Kafka", productId);
                return;
            }

            // For created/updated/status-changed: parse the full product or re-fetch from index dto
            ProductIndexDTO dto = objectMapper.treeToValue(node, ProductIndexDTO.class);
            if (dto.getId() == null) {
                dto.setId(productId);
            }
            searchService.updateProduct(dto);
            log.info("Updated product id={} in index via Kafka topic={}", productId, topic);

        } catch (Exception e) {
            log.error("Failed to process product event topic={}: {}", topic, e.getMessage(), e);
        }
    }

    @KafkaListener(
            topics = "product.deleted",
            groupId = "${spring.kafka.consumer.group-id:search-service-group}"
    )
    public void onProductDeleted(@Payload String payload,
                                 @Header(KafkaHeaders.OFFSET) long offset,
                                 @Header(KafkaHeaders.RECEIVED_PARTITION) int partition) {
        String idempotencyKey = IDEMPOTENCY_PREFIX + "product.deleted:" + partition + ":" + offset;
        Boolean isNew = redisTemplate.opsForValue().setIfAbsent(idempotencyKey, "1", Duration.ofDays(1));
        if (!Boolean.TRUE.equals(isNew)) {
            return;
        }
        try {
            JsonNode node = objectMapper.readTree(payload);
            Long productId = node.path("productId").asLong(-1);
            if (productId >= 0) {
                searchService.deleteProduct(String.valueOf(productId));
                log.info("Deleted product id={} from ES index", productId);
            }
        } catch (Exception e) {
            log.error("Failed to process product.deleted: {}", e.getMessage(), e);
        }
    }
}
