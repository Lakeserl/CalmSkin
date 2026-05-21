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

import java.util.List;

@Slf4j
@Component("inventoryServiceApiClient")
@RequiredArgsConstructor
public class InventoryServiceClient {

    private final RestClient inventoryServiceClient;

    @CircuitBreaker(name = "inventory-service", fallbackMethod = "fallbackCheckStockBatch")
    public List<StockCheckItem> checkStockBatch(List<StockCheckRequestItem> items) {
        log.info("Performing synchronous stock check-batch for size={} against inventory-service", items.size());
        return inventoryServiceClient.post()
                .uri("/internal/inventory/check-batch")
                .contentType(MediaType.APPLICATION_JSON)
                .body(items)
                .retrieve()
                .body(new ParameterizedTypeReference<List<StockCheckItem>>() {});
    }

    public List<StockCheckItem> fallbackCheckStockBatch(List<StockCheckRequestItem> items, Throwable t) {
        log.error("Fallback checkStockBatch due to: {}", t.getMessage());
        throw new ServiceUnavailableException("Inventory service is temporarily unavailable. " + t.getMessage());
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StockCheckRequestItem {
        private Long productId;
        private Long variantId;
        private Integer quantity;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class StockCheckItem {
        private Long productId;
        private Long variantId;
        private boolean available;
        private Integer quantityAvailable;
    }
}
