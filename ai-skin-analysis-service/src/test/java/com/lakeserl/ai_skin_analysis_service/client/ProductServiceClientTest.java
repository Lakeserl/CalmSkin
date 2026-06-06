package com.lakeserl.ai_skin_analysis_service.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lakeserl.ai_skin_analysis_service.dto.response.RecommendedProductDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * NEW-3 regression: a product-service outage must degrade to an empty list, never throw —
 * otherwise the already-paid-for Gemini analysis is lost when product lookup fails.
 */
class ProductServiceClientTest {

    private RestTemplate restTemplate;
    private ProductServiceClient client;

    @BeforeEach
    void setUp() {
        restTemplate = mock(RestTemplate.class);
        client = new ProductServiceClient(restTemplate, new ObjectMapper());
        ReflectionTestUtils.setField(client, "productServiceUrl", "http://product-service");
        ReflectionTestUtils.setField(client, "internalSecret", "secret");
    }

    @Test
    void returnsEmptyListWhenProductServiceFails() {
        when(restTemplate.exchange(any(String.class), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class)))
                .thenThrow(new RuntimeException("connection refused"));

        List<RecommendedProductDTO> result = client.findBySkinProfile("OILY", List.of("ACNE"));

        assertThat(result).isEmpty();
    }
}
