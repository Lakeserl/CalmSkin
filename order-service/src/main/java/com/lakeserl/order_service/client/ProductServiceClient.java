package com.lakeserl.order_service.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.lakeserl.order_service.exception.ServiceUnavailableException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductServiceClient {

    private final RestClient productServiceClient;

    @CircuitBreaker(name = "product-service", fallbackMethod = "fallbackValidateProducts")
    public List<ProductValidationItem> validateProducts(List<ProductValidationRequestItem> items) {
        log.info("Validating products with size={} against product-service", items.size());
        return productServiceClient.post()
                .uri("/internal/products/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .body(items)
                .retrieve()
                .body(new ParameterizedTypeReference<List<ProductValidationItem>>() {});
    }

    public List<ProductValidationItem> fallbackValidateProducts(List<ProductValidationRequestItem> items, Throwable t) {
        log.error("Fallback validateProducts due to: {}", t.getMessage());
        throw new ServiceUnavailableException("Product service is temporarily unavailable. " + t.getMessage());
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductValidationRequestItem {
        private Long productId;
        private Long variantId;
        private Integer quantity;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ProductValidationItem {
        private Long productId;
        private Long variantId;
        private String productName;
        private String productSku;
        private String variantName;
        private String productImageUrl;
        private String brandName;
        private BigDecimal unitPrice;
        private boolean available;
    }
}
