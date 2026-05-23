package com.lakeserl.promotion_service.client;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Calls user-service internal APIs. Used for birthday vouchers (which user has
 * a birthday today) and segment targeting (account age / loyalty tier).
 * Fallbacks degrade gracefully: a failed lookup must not break the promotion
 * flow, it just means the user is treated as not matching.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserServiceClient {

    private final RestClient userServiceRestClient;

    /** Returns ids of users whose date of birth falls on the given month/day. */
    @CircuitBreaker(name = "user-service", fallbackMethod = "fallbackBirthday")
    public List<UUID> findUserIdsByBirthday(int month, int day) {
        ApiEnvelope<List<UUID>> response = userServiceRestClient.get()
                .uri(uri -> uri.path("/internal/users/by-birthday")
                        .queryParam("month", month)
                        .queryParam("day", day)
                        .build())
                .retrieve()
                .body(new ParameterizedTypeReference<ApiEnvelope<List<UUID>>>() {});
        return response != null && response.data() != null ? response.data() : List.of();
    }

    List<UUID> fallbackBirthday(int month, int day, Throwable t) {
        log.error("user-service by-birthday lookup failed ({}/{}): {}", month, day, t.getMessage());
        return List.of();
    }

    /** Loads a user; returns null when the user cannot be fetched. */
    @CircuitBreaker(name = "user-service", fallbackMethod = "fallbackGetUser")
    public UserView getUser(UUID userId) {
        ApiEnvelope<UserView> response = userServiceRestClient.get()
                .uri("/internal/users/{id}", userId)
                .retrieve()
                .body(new ParameterizedTypeReference<ApiEnvelope<UserView>>() {});
        return response != null ? response.data() : null;
    }

    UserView fallbackGetUser(UUID userId, Throwable t) {
        log.error("user-service getUser failed for {}: {}", userId, t.getMessage());
        return null;
    }

    /** Subset of user-service ApiResponse needed here. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ApiEnvelope<T>(boolean success, String message, T data) {
    }

    /** Subset of user-service UserDTO needed for segment targeting. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record UserView(UUID id, LocalDate dateOfBirth, LocalDateTime createdAt, String status) {
    }
}
