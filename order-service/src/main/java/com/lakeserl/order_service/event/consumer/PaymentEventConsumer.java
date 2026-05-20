package com.lakeserl.order_service.event.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lakeserl.order_service.entity.OrderPaymentInfo;
import com.lakeserl.order_service.entity.ProcessedKafkaEvent;
import com.lakeserl.order_service.enums.OrderStatus;
import com.lakeserl.order_service.enums.PaymentStatus;
import com.lakeserl.order_service.event.payload.inbound.PaymentCompletedEvent;
import com.lakeserl.order_service.event.payload.inbound.PaymentFailedEvent;
import com.lakeserl.order_service.repository.OrderPaymentInfoRepository;
import com.lakeserl.order_service.repository.OrderRepository;
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
public class PaymentEventConsumer {

    private final OrderRepository orderRepository;
    private final OrderPaymentInfoRepository paymentInfoRepository;
    private final OrderStatusService orderStatusService;
    private final ProcessedKafkaEventRepository processedKafkaEventRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "payment.completed", groupId = "order-service")
    @Transactional
    public void handlePaymentCompleted(ConsumerRecord<String, String> record, Acknowledgment ack) {
        String eventId = record.key() != null ? record.key() : UUID.randomUUID().toString();
        log.info("Received payment.completed: key={}, value={}", record.key(), record.value());

        if (processedKafkaEventRepository.existsById(eventId)) {
            ack.acknowledge();
            return;
        }

        try {
            PaymentCompletedEvent event = objectMapper.readValue(record.value(), PaymentCompletedEvent.class);
            Long orderId = Long.parseLong(event.orderId());

            orderRepository.findById(orderId).ifPresent(order -> {
                if (order.getStatus() == OrderStatus.CONFIRMED || order.getStatus() == OrderStatus.PENDING) {
                    // Update order status: CONFIRMED -> PAID, then PAID -> PREPARING
                    orderStatusService.transitionTo(order, OrderStatus.PAID, "system", "Payment completed via " + event.paymentMethod());
                    orderStatusService.transitionTo(order, OrderStatus.PREPARING, "system", "Order is being prepared");

                    // Update payment info
                    paymentInfoRepository.findByOrderId(orderId).ifPresent(payment -> {
                        payment.setPaymentStatus(PaymentStatus.COMPLETED);
                        payment.setTransactionId(event.transactionId());
                        payment.setPaidAt(LocalDateTime.now());
                        paymentInfoRepository.save(payment);
                    });
                }
            });

            processedKafkaEventRepository.save(new ProcessedKafkaEvent(eventId, "payment.completed", null));
            ack.acknowledge();
        } catch (IOException | NumberFormatException ex) {
            log.error("Error processing payment.completed event", ex);
            throw new IllegalArgumentException("Failed to process event", ex);
        }
    }

    @KafkaListener(topics = "payment.failed", groupId = "order-service")
    @Transactional
    public void handlePaymentFailed(ConsumerRecord<String, String> record, Acknowledgment ack) {
        String eventId = record.key() != null ? record.key() : UUID.randomUUID().toString();
        log.info("Received payment.failed: key={}, value={}", record.key(), record.value());

        if (processedKafkaEventRepository.existsById(eventId)) {
            ack.acknowledge();
            return;
        }

        try {
            PaymentFailedEvent event = objectMapper.readValue(record.value(), PaymentFailedEvent.class);
            Long orderId = Long.parseLong(event.orderId());

            orderRepository.findById(orderId).ifPresent(order -> {
                if (order.getStatus() == OrderStatus.CONFIRMED || order.getStatus() == OrderStatus.PENDING) {
                    orderStatusService.transitionTo(order, OrderStatus.CANCELLED, "system", "Payment failed: " + event.reason());
                    order.setCancelReason("Payment failed: " + event.reason());
                    orderRepository.save(order);

                    paymentInfoRepository.findByOrderId(orderId).ifPresent(payment -> {
                        payment.setPaymentStatus(PaymentStatus.FAILED);
                        paymentInfoRepository.save(payment);
                    });
                }
            });

            processedKafkaEventRepository.save(new ProcessedKafkaEvent(eventId, "payment.failed", null));
            ack.acknowledge();
        } catch (IOException | NumberFormatException ex) {
            log.error("Error processing payment.failed event", ex);
            throw new IllegalArgumentException("Failed to process event", ex);
        }
    }
}
