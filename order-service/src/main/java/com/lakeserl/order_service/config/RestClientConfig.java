package com.lakeserl.order_service.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Value("${app.internal.secret:${internal.secret:}}")
    private String internalSecret;

    @Bean
    @LoadBalanced
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder()
                .defaultHeader("X-Internal-Secret", internalSecret);
    }

    @Bean
    public RestClient userServiceClient(RestClient.Builder builder) {
        return builder.baseUrl("http://USER-SERVICE").build();
    }

    @Bean
    public RestClient productServiceClient(RestClient.Builder builder) {
        return builder.baseUrl("http://PRODUCT-SERVICE").build();
    }

    @Bean
    public RestClient inventoryServiceClient(RestClient.Builder builder) {
        return builder.baseUrl("http://INVENTORY-SERVICE").build();
    }

    @Bean
    public RestClient promotionServiceClient(RestClient.Builder builder) {
        return builder.baseUrl("http://PROMOTION-SERVICE").build();
    }
}
