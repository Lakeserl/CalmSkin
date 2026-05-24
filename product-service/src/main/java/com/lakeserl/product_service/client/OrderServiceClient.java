package com.lakeserl.product_service.client;

import com.lakeserl.product_service.dto.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Internal client for fetching order history data from order-service.
 * Used by recommendation algorithm to exclude already-purchased products and
 * compute brand affinity from purchase history.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderServiceClient {

    private final RestClient orderServiceClient;

    /**
     * Returns distinct productIds the user has purchased in DELIVERED orders (last 365 days).
     * Returns empty list on any error so recommendations can still proceed.
     */
    public List<Long> getPurchasedProductIds(UUID userId) {
        try {
            ApiResponse<List<Long>> response = orderServiceClient.get()
                    .uri("/internal/orders/users/{userId}/purchased-product-ids", userId)
                    .retrieve()
                    .body(new ParameterizedTypeReference<ApiResponse<List<Long>>>() {});
            return response != null && response.getData() != null ? response.getData() : Collections.emptyList();
        } catch (Exception e) {
            log.warn("Failed to fetch purchased product IDs for userId={}: {}", userId, e.getMessage());
            return Collections.emptyList();
        }
    }
}
