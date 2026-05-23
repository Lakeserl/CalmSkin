package com.lakeserl.user_service.event;

import java.util.Map;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendUserRegistered(String userId, String email) {
        send("user.registered", Map.of("userId", userId, "email", email));
    }

    public void sendUserEmailVerified(String userId, String email) {
        send("user.email-verified", Map.of("userId", userId, "email", email));
    }

    public void sendPasswordReset(String userId, String email, String otp) {
        send("user.password-reset", Map.of("userId", userId, "email", email, "otp", otp));
    }

    public void sendUserLoggedIn(String userId, String ipAddress) {
        send("user.logged-in", Map.of("userId", userId, "ipAddress", ipAddress));
    }

    public void sendUserBanned(String userId, String email) {
        send("user.banned", Map.of("userId", userId, "email", email));
    }

    private void send(String topic, Object payload) {
        kafkaTemplate.send(topic, payload).whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to publish Kafka event: topic={}, error={}", topic, ex.getMessage(), ex);
            } else {
                log.debug("Kafka event sent: topic={}, partition={}, offset={}",
                        topic, result.getRecordMetadata().partition(), result.getRecordMetadata().offset());
            }
        });
    }
}
