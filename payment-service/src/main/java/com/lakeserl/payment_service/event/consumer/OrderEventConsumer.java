package com.lakeserl.payment_service.event.consumer;

import java.io.IOException;
import java.util.UUID;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lakeserl.payment_service.event.payload.inbound.OrderConfirmedEvent;
import com.lakeserl.payment_service.event.payload.inbound.OrderCancelledEvent;
import com.lakeserl.payment_service.models.entity.ProcessedKafkaEvent;
import com.lakeserl.payment_service.repository.ProcessedKafkaEventRepository;
import com.lakeserl.payment_service.service.PaymentService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class OrderEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderEventConsumer.class);

    private final ProcessedKafkaEventRepository processedKafkaEventRepository;
    private final PaymentService paymentService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "order.confirmed", groupId = "payment-service")
    @Transactional
    public void handleOrderConfirmed(ConsumerRecord<String, String> record, Acknowledgment ack) {
        String eventId = record.topic() + ":" + (record.key() != null ? record.key() : UUID.randomUUID().toString());
        log.info("Received order.confirmed: key={}, value={}", record.key(), record.value());

        if (processedKafkaEventRepository.existsById(eventId)) {
            log.info("Event key={} was already processed, skipping", eventId);
            ack.acknowledge();
            return;
        }

        try {
            OrderConfirmedEvent event = objectMapper.readValue(record.value(), OrderConfirmedEvent.class);
            paymentService.processOrderConfirmed(event);

            ProcessedKafkaEvent processedEvent = ProcessedKafkaEvent.builder()
                    .eventId(eventId)
                    .eventType("order.confirmed")
                    .build();
            processedKafkaEventRepository.save(processedEvent);
            
            ack.acknowledge();
            log.info("Successfully processed order.confirmed for eventId={}", eventId);
        } catch (IOException e) {
            log.error("Failed to parse order.confirmed event value", e);
            throw new IllegalArgumentException("Failed to process event value due to serialization error", e);
        } catch (Exception e) {
            log.error("Unexpected error handling order.confirmed event", e);
            throw e;
        }
    }

    @KafkaListener(topics = "order.cancelled", groupId = "payment-service")
    @Transactional
    public void handleOrderCancelled(ConsumerRecord<String, String> record, Acknowledgment ack) {
        String eventId = record.topic() + ":" + (record.key() != null ? record.key() : UUID.randomUUID().toString());
        log.info("Received order.cancelled: key={}, value={}", record.key(), record.value());

        if (processedKafkaEventRepository.existsById(eventId)) {
            log.info("Event key={} was already processed, skipping", eventId);
            ack.acknowledge();
            return;
        }

        try {
            OrderCancelledEvent event = objectMapper.readValue(record.value(), OrderCancelledEvent.class);
            paymentService.processOrderCancelled(event);

            ProcessedKafkaEvent processedEvent = ProcessedKafkaEvent.builder()
                    .eventId(eventId)
                    .eventType("order.cancelled")
                    .build();
            processedKafkaEventRepository.save(processedEvent);
            
            ack.acknowledge();
            log.info("Successfully processed order.cancelled for eventId={}", eventId);
        } catch (IOException e) {
            log.error("Failed to parse order.cancelled event value", e);
            throw new IllegalArgumentException("Failed to process event value due to serialization error", e);
        } catch (Exception e) {
            log.error("Unexpected error handling order.cancelled event", e);
            throw e;
        }
    }
}
