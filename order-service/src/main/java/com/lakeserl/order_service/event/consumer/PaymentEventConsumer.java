package com.lakeserl.order_service.event.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
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
        String eventId = record.key() != null
                ? record.topic() + ":" + record.key()
                : record.topic() + ":" + record.partition() + ":" + record.offset();
        log.info("Received payment.completed: key={}, eventId={}", record.key(), eventId);

        if (processedKafkaEventRepository.existsById(eventId)) {
            ack.acknowledge();
            return;
        }

        try {
            PaymentCompletedEvent event = objectMapper.readValue(record.value(), PaymentCompletedEvent.class);
            Long orderId = Long.parseLong(event.orderId());

            orderRepository.findById(orderId).ifPresent(order -> {
                if (order.getStatus() == OrderStatus.CONFIRMED || order.getStatus() == OrderStatus.PENDING) {
                    // Single atomic transition: skip PAID as intermediate durable state to avoid
                    // the order getting stuck in PAID if the second transition fails.
                    orderStatusService.transitionTo(order, OrderStatus.PREPARING, "system",
                            "Payment completed via " + event.paymentMethod() + ". Order is being prepared.");

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
            log.error("Error processing payment.completed event: eventId={}", eventId, ex);
            // Do not ack — let Kafka retry this message
        }
    }

    @KafkaListener(topics = "payment.failed", groupId = "order-service")
    @Transactional
    public void handlePaymentFailed(ConsumerRecord<String, String> record, Acknowledgment ack) {
        String eventId = record.key() != null
                ? record.topic() + ":" + record.key()
                : record.topic() + ":" + record.partition() + ":" + record.offset();
        log.info("Received payment.failed: key={}, eventId={}", record.key(), eventId);

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
            log.error("Error processing payment.failed event: eventId={}", eventId, ex);
            // Do not ack — let Kafka retry this message
        }
    }
}
