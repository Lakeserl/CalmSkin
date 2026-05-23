package com.lakeserl.promotion_service.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * RestClient beans for the two services promotion-service calls: user-service
 * (birthday + account data) and inventory-service (free-gift stock holds).
 * The internal secret is attached to every request so the callee's
 * InternalApiSecurityFilter accepts it.
 */
@Configuration
public class RestClientConfig {

    @Value("${app.internal-secret:}")
    private String internalSecret;

    @Bean
    @LoadBalanced
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder()
                .defaultHeader("X-Internal-Secret", internalSecret);
    }

    @Bean
    public RestClient userServiceRestClient(RestClient.Builder builder) {
        return builder.baseUrl("http://USER-SERVICE").build();
    }

    @Bean
    public RestClient inventoryServiceRestClient(RestClient.Builder builder) {
        return builder.baseUrl("http://INVENTORY-SERVICE").build();
    }
}
