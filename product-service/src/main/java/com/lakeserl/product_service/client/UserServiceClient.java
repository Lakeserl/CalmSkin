package com.lakeserl.product_service.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.lakeserl.product_service.dto.response.ApiResponse;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Internal client for fetching user skin profile and wishlist product IDs.
 * Uses Eureka load-balanced RestClient with X-Internal-Secret header.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserServiceClient {

    private final RestClient userServiceClient;

    /**
     * Returns the user's skin profile. Returns null if the user has no skin profile set.
     */
    public SkinProfileData getSkinProfile(UUID userId) {
        try {
            ApiResponse<SkinProfileData> response = userServiceClient.get()
                    .uri("/internal/users/skin-profiles/{userId}", userId)
                    .retrieve()
                    .body(new ParameterizedTypeReference<ApiResponse<SkinProfileData>>() {});
            return response != null ? response.getData() : null;
        } catch (HttpClientErrorException.NotFound e) {
            log.debug("No skin profile for userId={}", userId);
            return null;
        } catch (Exception e) {
            log.warn("Failed to fetch skin profile for userId={}: {}", userId, e.getMessage());
            return null;
        }
    }

    /**
     * Returns the user's wishlist product IDs.
     * Returns empty list on error so recommendations can still proceed without wishlist data.
     */
    public List<Long> getWishlistProductIds(UUID userId) {
        try {
            ApiResponse<List<Long>> response = userServiceClient.get()
                    .uri("/internal/users/wishlists/{userId}/product-ids", userId)
                    .retrieve()
                    .body(new ParameterizedTypeReference<ApiResponse<List<Long>>>() {});
            return response != null && response.getData() != null ? response.getData() : Collections.emptyList();
        } catch (Exception e) {
            log.warn("Failed to fetch wishlist for userId={}: {}", userId, e.getMessage());
            return Collections.emptyList();
        }
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SkinProfileData {
        private String skinType;           // matches SkinType enum name
        private List<String> skinConcerns;
        private List<String> allergies;
    }
}
