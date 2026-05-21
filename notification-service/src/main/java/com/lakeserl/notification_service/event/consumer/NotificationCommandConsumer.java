package com.lakeserl.notification_service.event.consumer;

import java.util.List;
import java.util.UUID;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lakeserl.notification_service.event.payload.NotificationCommand;
import com.lakeserl.notification_service.service.IdempotencyService;
import com.lakeserl.notification_service.service.NotificationDeliveryService;
import com.lakeserl.notification_service.service.NotificationDispatcher;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Consumes the priority dispatch lanes. Each lane has its own listener method
 * (hence its own consumer container and threads) so a bulk flood can never
 * delay a CRITICAL message. Idempotency is keyed purely on the command
 * dedupKey, so a redelivered or logically duplicated command is dropped once.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationCommandConsumer {

    private static final String IDEM_SCOPE = "cmd";

    private final ObjectMapper objectMapper;
    private final IdempotencyService idempotencyService;
    private final NotificationDispatcher dispatcher;
    private final NotificationDeliveryService deliveryService;

    @KafkaListener(topics = "notification.dispatch.critical", groupId = "notification-service")
    public void onCritical(ConsumerRecord<String, String> record, Acknowledgment ack) {
        process(record, ack);
    }

    @KafkaListener(topics = "notification.dispatch.high", groupId = "notification-service")
    public void onHigh(ConsumerRecord<String, String> record, Acknowledgment ack) {
        process(record, ack);
    }

    @KafkaListener(topics = "notification.dispatch.normal", groupId = "notification-service")
    public void onNormal(ConsumerRecord<String, String> record, Acknowledgment ack) {
        process(record, ack);
    }

    @KafkaListener(topics = "notification.dispatch.bulk", groupId = "notification-service")
    public void onBulk(ConsumerRecord<String, String> record, Acknowledgment ack) {
        process(record, ack);
    }

    private void process(ConsumerRecord<String, String> record, Acknowledgment ack) {
        String dedupKey = record.key() != null ? record.key() : UUID.randomUUID().toString();
        if (idempotencyService.isDuplicate(IDEM_SCOPE, dedupKey)) {
            log.debug("Command dedupKey={} already dispatched, skipping", dedupKey);
            ack.acknowledge();
            return;
        }
        try {
            NotificationCommand command = objectMapper.readValue(record.value(), NotificationCommand.class);
            List<Long> immediate = dispatcher.dispatch(command);
            idempotencyService.markProcessed(IDEM_SCOPE, dedupKey);
            for (Long notificationId : immediate) {
                deliveryService.deliver(notificationId);
            }
            ack.acknowledge();
            log.info("Dispatched command dedupKey={} immediateDeliveries={}", dedupKey, immediate.size());
        } catch (Exception ex) {
            log.error("Failed to process notification command dedupKey={}", dedupKey, ex);
            throw new IllegalStateException("Notification command processing failed", ex);
        }
    }
}
