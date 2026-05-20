package com.lakeserl.order_service.event.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lakeserl.order_service.entity.OrderShippingInfo;
import com.lakeserl.order_service.entity.ProcessedKafkaEvent;
import com.lakeserl.order_service.enums.OrderStatus;
import com.lakeserl.order_service.event.payload.inbound.ShippingUpdatedEvent;
import com.lakeserl.order_service.repository.OrderRepository;
import com.lakeserl.order_service.repository.OrderShippingInfoRepository;
import com.lakeserl.order_service.repository.ProcessedKafkaEventRepository;
import com.lakeserl.order_service.service.OrderStatusService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class ShippingEventConsumer {

    private final OrderRepository orderRepository;
    private final OrderShippingInfoRepository shippingInfoRepository;
    private final OrderStatusService orderStatusService;
    private final ProcessedKafkaEventRepository processedKafkaEventRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "shipping.updated", groupId = "order-service")
    @Transactional
    public void handleShippingUpdated(ConsumerRecord<String, String> record, Acknowledgment ack) {
        String eventId = record.key() != null ? record.key() : UUID.randomUUID().toString();
        log.info("Received shipping.updated: key={}, value={}", record.key(), record.value());

        if (processedKafkaEventRepository.existsById(eventId)) {
            ack.acknowledge();
            return;
        }

        try {
            ShippingUpdatedEvent event = objectMapper.readValue(record.value(), ShippingUpdatedEvent.class);
            Long orderId = Long.parseLong(event.orderId());

            orderRepository.findById(orderId).ifPresent(order -> {
                String status = event.shippingStatus().toUpperCase();
                
                if ("SHIPPED".equals(status) && order.getStatus() == OrderStatus.PREPARING) {
                    orderStatusService.transitionTo(order, OrderStatus.SHIPPING, "system", "Package handed over to shipping provider");
                } else if ("DELIVERED".equals(status) && order.getStatus() == OrderStatus.SHIPPING) {
                    orderStatusService.transitionTo(order, OrderStatus.DELIVERED, "system", "Package delivered successfully to customer");
                }

                // Update shipping info
                shippingInfoRepository.findByOrderId(orderId).ifPresent(shipping -> {
                    shipping.setShippingProvider(event.shippingProvider());
                    shipping.setTrackingNumber(event.trackingNumber());
                    shipping.setShippingStatus(event.shippingStatus());
                    if (event.estimatedDelivery() != null && !event.estimatedDelivery().isBlank()) {
                        shipping.setEstimatedDelivery(LocalDateTime.parse(event.estimatedDelivery()));
                    }
                    if ("DELIVERED".equals(status)) {
                        shipping.setActualDelivery(LocalDateTime.now());
                    }
                    shippingInfoRepository.save(shipping);
                });
            });

            processedKafkaEventRepository.save(new ProcessedKafkaEvent(eventId, "shipping.updated", null));
            ack.acknowledge();
        } catch (IOException | NumberFormatException ex) {
            log.error("Error processing shipping.updated event", ex);
            throw new IllegalArgumentException("Failed to process event", ex);
        }
    }
}
