package com.lakeserl.notification_service.event.producer;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lakeserl.notification_service.event.payload.NotificationCommand;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Publishes a {@link NotificationCommand} onto the Kafka priority topic that
 * matches its priority. The command's {@code dedupKey} is the message key, so
 * consumer idempotency also drops logically identical commands.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationCommandProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public void publish(NotificationCommand command) {
        try {
            String payload = objectMapper.writeValueAsString(command);
            kafkaTemplate.send(command.priority().dispatchTopic(), command.dedupKey(), payload);
            log.debug("Published notification command dedupKey={} topic={}",
                    command.dedupKey(), command.priority().dispatchTopic());
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialise notification command", ex);
        }
    }
}
