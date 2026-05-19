package com.lakeserl.inventory_service.event.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lakeserl.inventory_service.dto.request.ReserveStockRequest;
import com.lakeserl.inventory_service.event.payload.OrderCancelledEvent;
import com.lakeserl.inventory_service.event.payload.OrderCompletedEvent;
import com.lakeserl.inventory_service.event.payload.OrderCreatedEvent;
import com.lakeserl.inventory_service.event.producer.InventoryEventProducer;
import com.lakeserl.inventory_service.exception.InsufficientStockException;
import com.lakeserl.inventory_service.exception.ReservationAlreadyProcessedException;
import com.lakeserl.inventory_service.exception.ReservationNotFoundException;
import com.lakeserl.inventory_service.entity.ProcessedKafkaEvent;
import com.lakeserl.inventory_service.repository.ProcessedKafkaEventRepository;
import com.lakeserl.inventory_service.service.StockReservationService;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventConsumer {
    private final StockReservationService stockReservationService;
    private final ProcessedKafkaEventRepository processedKafkaEventRepository;
    private final InventoryEventProducer inventoryEventProducer;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "order.created", groupId = "inventory-service")
    @Transactional(noRollbackFor = InsufficientStockException.class)
    public void handleOrderCreated(ConsumerRecord<String, String> record, Acknowledgment ack) {
        OrderCreatedEvent event = readEvent(record.value(), OrderCreatedEvent.class);
        String orderId = resolveOrderId(record.key(), event.orderId());

        if (processedKafkaEventRepository.existsById(orderId)) {
            ack.acknowledge();
            return;
        }

        try {
            ReserveStockRequest request = toReserveRequest(event, orderId);
            stockReservationService.reserveStock(request);
            processedKafkaEventRepository.save(ProcessedKafkaEvent.builder()
                    .eventId(orderId)
                    .eventType("order.created")
                    .build());
            ack.acknowledge();
        } catch (InsufficientStockException ex) {
            inventoryEventProducer.publish("inventory.insufficient", orderId,
                    Map.of("orderId", orderId, "reason", ex.getMessage()));
            processedKafkaEventRepository.save(ProcessedKafkaEvent.builder()
                    .eventId(orderId)
                    .eventType("order.created")
                    .build());
            ack.acknowledge();
        }
    }

    @KafkaListener(topics = "order.cancelled", groupId = "inventory-service")
    @Transactional
    public void handleOrderCancelled(ConsumerRecord<String, String> record, Acknowledgment ack) {
        OrderCancelledEvent event = readEvent(record.value(), OrderCancelledEvent.class);
        String orderId = resolveOrderId(record.key(), event.orderId());

        if (processedKafkaEventRepository.existsById(orderId)) {
            ack.acknowledge();
            return;
        }

        try {
            stockReservationService.releaseStock(orderId);
        } catch (ReservationNotFoundException | ReservationAlreadyProcessedException ex) {
            log.info("Skip release for orderId={}: {}", orderId, ex.getMessage());
        }

        processedKafkaEventRepository.save(ProcessedKafkaEvent.builder()
                .eventId(orderId)
                .eventType("order.cancelled")
                .build());
        ack.acknowledge();
    }

    @KafkaListener(topics = "order.completed", groupId = "inventory-service")
    @Transactional
    public void handleOrderCompleted(ConsumerRecord<String, String> record, Acknowledgment ack) {
        OrderCompletedEvent event = readEvent(record.value(), OrderCompletedEvent.class);
        String orderId = resolveOrderId(record.key(), event.orderId());

        if (processedKafkaEventRepository.existsById(orderId)) {
            ack.acknowledge();
            return;
        }

        try {
            stockReservationService.confirmStock(orderId);
        } catch (ReservationNotFoundException | ReservationAlreadyProcessedException ex) {
            log.info("Skip confirm for orderId={}: {}", orderId, ex.getMessage());
        }

        processedKafkaEventRepository.save(ProcessedKafkaEvent.builder()
                .eventId(orderId)
                .eventType("order.completed")
                .build());
        ack.acknowledge();
    }

    private ReserveStockRequest toReserveRequest(OrderCreatedEvent event, String orderId) {
        List<ReserveStockRequest.ReserveItem> items = event.items().stream()
                .map(item -> new ReserveStockRequest.ReserveItem(
                        item.productId(),
                        item.variantId(),
                        item.quantity()))
                .toList();
        return new ReserveStockRequest(orderId, items);
    }

    private String resolveOrderId(String recordKey, String eventOrderId) {
        return recordKey == null || recordKey.isBlank() ? eventOrderId : recordKey;
    }

    private <T> T readEvent(String value, Class<T> type) {
        try {
            return objectMapper.readValue(value, type);
        } catch (IOException ex) {
            throw new IllegalArgumentException("Failed to parse kafka event", ex);
        }
    }
}
