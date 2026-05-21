package com.lakeserl.notification_service.event.consumer;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lakeserl.notification_service.enums.NotificationCategory;
import com.lakeserl.notification_service.enums.NotificationChannel;
import com.lakeserl.notification_service.enums.NotificationPriority;
import com.lakeserl.notification_service.event.payload.NotificationCommand;
import com.lakeserl.notification_service.event.producer.NotificationCommandProducer;
import com.lakeserl.notification_service.service.UserContactService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Consumes user.* events. Keeps the local contact cache fresh and emits the
 * matching account notifications. Account messages use the SECURITY_ALERTS
 * category so a user can never opt out of them.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserEventConsumer {

    private final ObjectMapper objectMapper;
    private final NotificationCommandProducer commandProducer;
    private final UserContactService userContactService;

    @KafkaListener(topics = {"user.registered", "user.email-verified", "user.password-reset", "user.banned"},
            groupId = "notification-service")
    public void handle(ConsumerRecord<String, String> record, Acknowledgment ack) {
        try {
            JsonNode node = objectMapper.readTree(record.value());
            UUID userId = EventJson.uuid(node, "userId");
            if (userId == null) {
                log.warn("Skipping {} event without userId", record.topic());
                ack.acknowledge();
                return;
            }
            String email = EventJson.text(node, "email");
            if (email != null) {
                userContactService.upsert(userId, email, null, null);
            }

            switch (record.topic()) {
                case "user.registered" -> commandProducer.publish(NotificationCommand.builder()
                        .dedupKey("user.registered:" + userId)
                        .userId(userId)
                        .category(NotificationCategory.SECURITY_ALERTS)
                        .priority(NotificationPriority.HIGH)
                        .templateCode("USER_REGISTERED")
                        .channels(List.of(NotificationChannel.EMAIL, NotificationChannel.IN_APP))
                        .title("Welcome to CalmSKIN")
                        .body("Your CalmSKIN account is ready. Start your skincare journey today.")
                        .referenceType("USER")
                        .referenceId(userId.toString())
                        .build());
                case "user.email-verified" -> commandProducer.publish(NotificationCommand.builder()
                        .dedupKey("user.email-verified:" + userId)
                        .userId(userId)
                        .category(NotificationCategory.SECURITY_ALERTS)
                        .priority(NotificationPriority.NORMAL)
                        .templateCode("USER_EMAIL_VERIFIED")
                        .channels(List.of(NotificationChannel.EMAIL, NotificationChannel.IN_APP))
                        .title("Email verified")
                        .body("Your email address has been verified successfully.")
                        .referenceType("USER")
                        .referenceId(userId.toString())
                        .build());
                case "user.password-reset" -> {
                    String otp = node.path("otp").asText("");
                    commandProducer.publish(NotificationCommand.builder()
                            .dedupKey("user.password-reset:" + userId + ":" + otp)
                            .userId(userId)
                            .category(NotificationCategory.SECURITY_ALERTS)
                            .priority(NotificationPriority.CRITICAL)
                            .templateCode("USER_PASSWORD_RESET")
                            .channels(List.of(NotificationChannel.EMAIL))
                            .title("Password reset code")
                            .body("Use this code to reset your password: " + otp)
                            .variables(Map.of("otp", otp))
                            .referenceType("USER")
                            .referenceId(userId.toString())
                            .build());
                }
                case "user.banned" -> commandProducer.publish(NotificationCommand.builder()
                        .dedupKey("user.banned:" + userId)
                        .userId(userId)
                        .category(NotificationCategory.SECURITY_ALERTS)
                        .priority(NotificationPriority.CRITICAL)
                        .templateCode("USER_BANNED")
                        .channels(List.of(NotificationChannel.EMAIL))
                        .title("Account suspended")
                        .body("Your CalmSKIN account has been suspended. Contact support for help.")
                        .referenceType("USER")
                        .referenceId(userId.toString())
                        .build());
                default -> log.warn("Unhandled user topic {}", record.topic());
            }
            ack.acknowledge();
        } catch (Exception ex) {
            log.error("Failed to handle user event from topic={}", record.topic(), ex);
            throw new IllegalStateException("User event processing failed", ex);
        }
    }
}
