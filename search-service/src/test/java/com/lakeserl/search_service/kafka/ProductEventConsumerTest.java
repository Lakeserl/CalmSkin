package com.lakeserl.search_service.kafka;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lakeserl.search_service.dto.ProductIndexDTO;
import com.lakeserl.search_service.service.SearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Kafka consumers must be idempotent: redelivery of the same (topic, partition, offset) must
 * not re-index. The Redis setIfAbsent guard is what turns a duplicate delivery into a no-op.
 */
class ProductEventConsumerTest {

    private SearchService searchService;
    private ValueOperations<String, String> valueOps;
    private ProductEventConsumer consumer;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        searchService = mock(SearchService.class);
        RedisTemplate<String, String> redisTemplate = mock(RedisTemplate.class);
        valueOps = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        // Match the Spring-Boot ObjectMapper bean, which ignores unknown JSON properties.
        ObjectMapper mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        consumer = new ProductEventConsumer(searchService, mapper, redisTemplate);
    }

    @Test
    void firstDeliveryIndexesTheProduct() {
        when(valueOps.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);

        consumer.onProductEvent("{\"productId\":1}", "product.created", 5L, 0);

        verify(searchService).updateProduct(any(ProductIndexDTO.class));
    }

    @Test
    void duplicateDeliveryIsSkipped() {
        when(valueOps.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(false);

        consumer.onProductEvent("{\"productId\":1}", "product.created", 5L, 0);

        verify(searchService, never()).updateProduct(any());
    }
}
