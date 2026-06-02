package com.lakeserl.ai_recommendation_service.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
public class OrderEventConsumer {

    private static final String TRENDING_KEY      = "rec:trending";
    private static final String IDEMPOTENCY_PREFIX = "rec:processed:";

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = "order.completed",
            groupId = "${spring.kafka.consumer.group-id:recommendation-service-group}"
    )
    public void onOrderCompleted(@Payload String payload,
                                 @Header(KafkaHeaders.OFFSET) long offset,
                                 @Header(KafkaHeaders.RECEIVED_PARTITION) int partition) {
        String idempotencyKey = IDEMPOTENCY_PREFIX + "order.completed:" + partition + ":" + offset;
        Boolean isNew = redisTemplate.opsForValue().setIfAbsent(idempotencyKey, "1", Duration.ofDays(2));
        if (!Boolean.TRUE.equals(isNew)) {
            return;
        }

        try {
            JsonNode root = objectMapper.readTree(payload);
            // order.completed payload has an "items" array with "productId" + "quantity"
            JsonNode items = root.path("items");
            if (!items.isArray()) {
                return;
            }
            for (JsonNode item : items) {
                long productId = item.path("productId").asLong(-1);
                int quantity   = item.path("quantity").asInt(1);
                if (productId > 0) {
                    redisTemplate.opsForZSet().incrementScore(TRENDING_KEY, String.valueOf(productId), quantity);
                }
            }
        } catch (Exception e) {
            log.error("Failed to process order.completed for trending: {}", e.getMessage(), e);
        }
    }
}
