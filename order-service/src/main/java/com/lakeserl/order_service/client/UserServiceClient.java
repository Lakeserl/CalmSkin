package com.lakeserl.order_service.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lakeserl.order_service.dto.response.ApiResponse;
import com.lakeserl.order_service.exception.ServiceUnavailableException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.UUID;

@Slf4j
@Component("userServiceApiClient")
@RequiredArgsConstructor
public class UserServiceClient {

    private final RestClient userServiceClient;

    @CircuitBreaker(name = "user-service", fallbackMethod = "fallbackGetAddress")
    public AddressResponse getAddress(UUID userId, UUID addressId) {
        log.info("Fetching addressId={} for userId={} from user-service", addressId, userId);
        ApiResponse<AddressResponse> response = userServiceClient.get()
                .uri("/internal/users/{userId}/addresses/{addressId}", userId, addressId)
                .retrieve()
                .body(new ParameterizedTypeReference<ApiResponse<AddressResponse>>() {});
        return response != null ? response.getData() : null;
    }

    @CircuitBreaker(name = "user-service", fallbackMethod = "fallbackGetPoints")
    public PointsResponse getPoints(UUID userId) {
        log.info("Fetching points for userId={} from user-service", userId);
        ApiResponse<PointsResponse> response = userServiceClient.get()
                .uri("/internal/users/{userId}/points", userId)
                .retrieve()
                .body(new ParameterizedTypeReference<ApiResponse<PointsResponse>>() {});
        return response != null ? response.getData() : null;
    }

    public AddressResponse fallbackGetAddress(UUID userId, UUID addressId, Throwable t) {
        log.error("Fallback getAddress userId={}, addressId={} due to: {}", userId, addressId, t.getMessage());
        throw new ServiceUnavailableException("User service address lookup is temporarily unavailable. " + t.getMessage());
    }

    public PointsResponse fallbackGetPoints(UUID userId, Throwable t) {
        log.error("Fallback getPoints userId={} due to: {}", userId, t.getMessage());
        throw new ServiceUnavailableException("User service points retrieval is temporarily unavailable. " + t.getMessage());
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AddressResponse {
        private String recipientName;
        private String phone;
        private String province;
        private String district;
        private String ward;
        private String street;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PointsResponse {
        @JsonProperty("totalPoints")
        private Integer points;
    }
}

