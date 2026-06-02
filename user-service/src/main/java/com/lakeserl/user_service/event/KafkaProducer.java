package com.lakeserl.user_service.event;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lakeserl.user_service.model.entity.OutboxEvent;
import com.lakeserl.user_service.repository.OutboxRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Writes Kafka events to the outbox_events table within the caller's transaction.
 * OutboxEventPublisher scheduler delivers them to Kafka asynchronously.
 * This guarantees atomicity between the business write and the event — Invariant §14.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaProducer {

    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public void sendUserRegistered(String userId, String email) {
        save("user.registered", userId, Map.of("userId", userId, "email", email));
    }

    public void sendUserEmailVerified(String userId, String email) {
        save("user.email-verified", userId, Map.of("userId", userId, "email", email));
    }

    public void sendPasswordReset(String userId, String email, String otp) {
        save("user.password-reset", userId, Map.of("userId", userId, "email", email, "otp", otp));
    }

    public void sendUserLoggedIn(String userId, String ipAddress) {
        save("user.logged-in", userId, Map.of("userId", userId, "ipAddress", ipAddress));
    }

    public void sendUserBanned(String userId, String email) {
        save("user.banned", userId, Map.of("userId", userId, "email", email));
    }

    public void sendSkinProfileUpdated(String userId) {
        save("user.skin-profile-updated", userId, Map.of("userId", userId));
    }

    private void save(String topic, String aggregateId, Map<String, Object> payload) {
        try {
            outboxRepository.save(OutboxEvent.builder()
                    .aggregateType("User")
                    .aggregateId(aggregateId)
                    .eventType(topic)
                    .payload(objectMapper.writeValueAsString(payload))
                    .build());
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize outbox payload for topic={} aggregateId={}", topic, aggregateId, e);
            throw new IllegalStateException("Outbox serialization failed for topic: " + topic, e);
        }
    }
}
