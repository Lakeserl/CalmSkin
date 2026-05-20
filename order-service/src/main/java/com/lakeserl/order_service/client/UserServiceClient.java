package com.lakeserl.order_service.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.lakeserl.order_service.exception.ServiceUnavailableException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserServiceClient {

    private final RestClient userServiceClient;

    @CircuitBreaker(name = "user-service", fallbackMethod = "fallbackGetAddress")
    public AddressResponse getAddress(Long userId, Long addressId) {
        log.info("Fetching addressId={} for userId={} from user-service", addressId, userId);
        return userServiceClient.get()
                .uri("/internal/users/{userId}/addresses/{addressId}", userId, addressId)
                .retrieve()
                .body(AddressResponse.class);
    }

    @CircuitBreaker(name = "user-service", fallbackMethod = "fallbackGetPoints")
    public PointsResponse getPoints(Long userId) {
        log.info("Fetching points for userId={} from user-service", userId);
        return userServiceClient.get()
                .uri("/internal/users/{userId}/points", userId)
                .retrieve()
                .body(PointsResponse.class);
    }

    public AddressResponse fallbackGetAddress(Long userId, Long addressId, Throwable t) {
        log.error("Fallback getAddress userId={}, addressId={} due to: {}", userId, addressId, t.getMessage());
        throw new ServiceUnavailableException("User service address lookup is temporarily unavailable. " + t.getMessage());
    }

    public PointsResponse fallbackGetPoints(Long userId, Throwable t) {
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
        private Integer points;
    }
}
