package com.lakeserl.shipping_service.event.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lakeserl.shipping_service.entity.ProcessedKafkaEvent;
import com.lakeserl.shipping_service.event.payload.inbound.OrderCancelledEvent;
import com.lakeserl.shipping_service.event.payload.inbound.OrderConfirmedEvent;
import com.lakeserl.shipping_service.exception.ShipmentNotFoundException;
import com.lakeserl.shipping_service.repository.ProcessedKafkaEventRepository;
import com.lakeserl.shipping_service.service.ShipmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventConsumer {

    private final ShipmentService shipmentService;
    private final ProcessedKafkaEventRepository processedKafkaEventRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "order.confirmed", groupId = "shipping-service")
    @Transactional
    public void handleOrderConfirmed(ConsumerRecord<String, String> record, Acknowledgment ack) {
        String eventId = record.topic() + ":" + (record.key() != null ? record.key() : UUID.randomUUID().toString());
        log.info("Received order.confirmed: key={}", record.key());

        if (processedKafkaEventRepository.existsById(eventId)) {
            ack.acknowledge();
            return;
        }

        try {
            OrderConfirmedEvent event = objectMapper.readValue(record.value(), OrderConfirmedEvent.class);
            shipmentService.createFromOrder(event);

            processedKafkaEventRepository.save(new ProcessedKafkaEvent(eventId, "order.confirmed"));
            ack.acknowledge();
        } catch (IOException ex) {
            log.error("Failed to parse order.confirmed payload", ex);
            // Bad payload — do not retry forever; throw so the configured DLT
            // recoverer can capture it.
            throw new IllegalArgumentException("Failed to parse order.confirmed event", ex);
        }
    }

    @KafkaListener(topics = "order.cancelled", groupId = "shipping-service")
    @Transactional
    public void handleOrderCancelled(ConsumerRecord<String, String> record, Acknowledgment ack) {
        String eventId = record.topic() + ":" + (record.key() != null ? record.key() : UUID.randomUUID().toString());
        log.info("Received order.cancelled: key={}", record.key());

        if (processedKafkaEventRepository.existsById(eventId)) {
            ack.acknowledge();
            return;
        }

        try {
            OrderCancelledEvent event = objectMapper.readValue(record.value(), OrderCancelledEvent.class);
            Long orderId = Long.parseLong(event.orderId());
            try {
                shipmentService.cancelByOrderId(orderId, event.reason() == null ? "Order cancelled" : event.reason());
            } catch (ShipmentNotFoundException ignored) {
                // Order was cancelled before a shipment was created — that's
                // valid (e.g. inventory insufficient path). Nothing to do.
                log.info("No shipment to cancel for orderId={} (likely cancelled before label creation)", orderId);
            }

            processedKafkaEventRepository.save(new ProcessedKafkaEvent(eventId, "order.cancelled"));
            ack.acknowledge();
        } catch (IOException | NumberFormatException ex) {
            log.error("Failed to process order.cancelled", ex);
            throw new IllegalArgumentException("Failed to parse order.cancelled event", ex);
        }
    }
}
