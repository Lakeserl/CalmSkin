package com.lakeserl.order_service.event.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lakeserl.order_service.entity.Order;
import com.lakeserl.order_service.entity.OrderItem;
import com.lakeserl.order_service.entity.ProcessedKafkaEvent;
import com.lakeserl.order_service.enums.OrderStatus;
import com.lakeserl.order_service.event.payload.inbound.InventoryInsufficientEvent;
import com.lakeserl.order_service.event.payload.inbound.InventoryReservedEvent;
import com.lakeserl.order_service.repository.OrderRepository;
import com.lakeserl.order_service.repository.ProcessedKafkaEventRepository;
import com.lakeserl.order_service.service.OrderStatusService;
import com.lakeserl.order_service.event.producer.OrderEventProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryEventConsumer {

    private final OrderRepository orderRepository;
    private final OrderStatusService orderStatusService;
    private final ProcessedKafkaEventRepository processedKafkaEventRepository;
    private final OrderEventProducer orderEventProducer;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "inventory.reserved", groupId = "order-service")
    @Transactional
    public void handleInventoryReserved(ConsumerRecord<String, String> record, Acknowledgment ack) {
        String eventId = record.topic() + ":" + (record.key() != null ? record.key() : UUID.randomUUID().toString());
        log.info("Received inventory.reserved: key={}, value={}", record.key(), record.value());

        if (processedKafkaEventRepository.existsById(eventId)) {
            ack.acknowledge();
            return;
        }

        try {
            InventoryReservedEvent event = objectMapper.readValue(record.value(), InventoryReservedEvent.class);
            Long orderId = Long.parseLong(event.orderId());

            orderRepository.findById(orderId).ifPresent(order -> {
                if (order.getStatus() == OrderStatus.PENDING) {
                    orderStatusService.transitionTo(order, OrderStatus.CONFIRMED, "system", "Inventory stock reserved successfully");

                    orderEventProducer.publish("order.confirmed", order.getId().toString(), buildOrderConfirmedPayload(order));
                    log.info("Published order.confirmed outbox event for orderId={}", orderId);
                }
            });

            processedKafkaEventRepository.save(new ProcessedKafkaEvent(eventId, "inventory.reserved", null));
            ack.acknowledge();
        } catch (IOException | NumberFormatException ex) {
            log.error("Error processing inventory.reserved event", ex);
            // Default DLQ handler will handle retries/DLQ
            throw new IllegalArgumentException("Failed to process event", ex);
        }
    }

    // Builds the order.confirmed payload. Original fields (orderId, orderNumber, userId, totalAmount,
    // paymentMethod) are preserved verbatim — payment-service deserializes them as a strict record.
    // Additive fields (shippingFee, shipping, items) are read by shipping-service to create labels
    // without an extra REST round-trip. Lazy collections are safe to traverse here: this method runs
    // inside the @KafkaListener's @Transactional, so the persistence session is open.
    private Map<String, Object> buildOrderConfirmedPayload(Order order) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("orderId", order.getId().toString());
        payload.put("orderNumber", order.getOrderNumber());
        payload.put("userId", order.getUserId());
        payload.put("totalAmount", order.getTotalAmount());
        payload.put("paymentMethod", order.getPaymentMethod().name());
        payload.put("shippingFee", order.getShippingFee());

        Map<String, Object> shipping = new LinkedHashMap<>();
        shipping.put("name", order.getShippingName());
        shipping.put("phone", order.getShippingPhone());
        shipping.put("street", order.getShippingStreet());
        shipping.put("ward", order.getShippingWard());
        shipping.put("district", order.getShippingDistrict());
        shipping.put("province", order.getShippingProvince());
        shipping.put("country", "VN");
        payload.put("shipping", shipping);

        List<Map<String, Object>> items = new ArrayList<>();
        for (OrderItem item : order.getItems()) {
            Map<String, Object> i = new LinkedHashMap<>();
            i.put("orderItemId", item.getId());
            i.put("productId", item.getProductId());
            i.put("variantId", item.getVariantId());
            i.put("productName", item.getProductName());
            i.put("productSku", item.getProductSku());
            i.put("quantity", item.getQuantity());
            i.put("unitPrice", item.getUnitPrice());
            i.put("subtotal", item.getSubtotal());
            items.add(i);
        }
        payload.put("items", items);
        return payload;
    }

    @KafkaListener(topics = "inventory.insufficient", groupId = "order-service")
    @Transactional
    public void handleInventoryInsufficient(ConsumerRecord<String, String> record, Acknowledgment ack) {
        String eventId = record.topic() + ":" + (record.key() != null ? record.key() : UUID.randomUUID().toString());
        log.info("Received inventory.insufficient: key={}, value={}", record.key(), record.value());

        if (processedKafkaEventRepository.existsById(eventId)) {
            ack.acknowledge();
            return;
        }

        try {
            InventoryInsufficientEvent event = objectMapper.readValue(record.value(), InventoryInsufficientEvent.class);
            Long orderId = Long.parseLong(event.orderId());

            orderRepository.findById(orderId).ifPresent(order -> {
                if (order.getStatus() == OrderStatus.PENDING) {
                    orderStatusService.transitionTo(order, OrderStatus.CANCELLED, "system", "Cancelled due to insufficient inventory stock: " + event.reason());
                    order.setCancelReason("Insufficient inventory stock: " + event.reason());
                    orderRepository.save(order);
                }
            });

            processedKafkaEventRepository.save(new ProcessedKafkaEvent(eventId, "inventory.insufficient", null));
            ack.acknowledge();
        } catch (IOException | NumberFormatException ex) {
            log.error("Error processing inventory.insufficient event", ex);
            throw new IllegalArgumentException("Failed to process event", ex);
        }
    }

    @KafkaListener(topics = "inventory.reservation-expired", groupId = "order-service")
    @Transactional
    public void handleInventoryReservationExpired(ConsumerRecord<String, String> record, Acknowledgment ack) {
        String eventId = record.topic() + ":" + (record.key() != null ? record.key() : UUID.randomUUID().toString());
        log.info("Received inventory.reservation-expired: key={}, value={}", record.key(), record.value());

        if (processedKafkaEventRepository.existsById(eventId)) {
            ack.acknowledge();
            return;
        }

        try {
            InventoryInsufficientEvent event = objectMapper.readValue(record.value(), InventoryInsufficientEvent.class);
            Long orderId = Long.parseLong(event.orderId());

            orderRepository.findById(orderId).ifPresent(order -> {
                if (order.getStatus() == OrderStatus.PENDING || order.getStatus() == OrderStatus.CONFIRMED) {
                    orderStatusService.transitionTo(order, OrderStatus.CANCELLED, "system", "Cancelled due to stock reservation timeout");
                    order.setCancelReason("Inventory stock reservation expired");
                    orderRepository.save(order);
                }
            });

            processedKafkaEventRepository.save(new ProcessedKafkaEvent(eventId, "inventory.reservation-expired", null));
            ack.acknowledge();
        } catch (IOException | NumberFormatException ex) {
            log.error("Error processing inventory.reservation-expired event", ex);
            throw new IllegalArgumentException("Failed to process event", ex);
        }
    }
}
