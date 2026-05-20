package com.lakeserl.payment_service.scheduler;

import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.lakeserl.payment_service.event.payload.outbound.PaymentFailedEvent;
import com.lakeserl.payment_service.event.producer.PaymentEventProducer;
import com.lakeserl.payment_service.models.entity.Payment;
import com.lakeserl.payment_service.models.enums.PaymentStatus;
import com.lakeserl.payment_service.repository.PaymentRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PaymentExpiryScheduler {

    private static final Logger log = LoggerFactory.getLogger(PaymentExpiryScheduler.class);
    private static final String REDIS_INITIATED_PREFIX = "payment:initiated:";

    private final PaymentRepository paymentRepository;
    private final PaymentEventProducer eventProducer;
    private final StringRedisTemplate redisTemplate;

    @Scheduled(fixedDelay = 60000) // check every 1 minute
    @Transactional
    public void cleanExpiredPayments() {
        LocalDateTime now = LocalDateTime.now();
        List<Payment> expiredPayments = paymentRepository.findByStatusAndExpiresAtBefore(
                PaymentStatus.PENDING, now
        );

        if (expiredPayments.isEmpty()) {
            return;
        }

        log.info("Found {} expired pending online payments to clean up", expiredPayments.size());

        for (Payment payment : expiredPayments) {
            try {
                payment.setStatus(PaymentStatus.FAILED);
                payment.setFailureReason("Payment session expired");
                paymentRepository.save(payment);

                // Publish payment.failed event
                eventProducer.publish("payment.failed", payment.getOrderId().toString(), new PaymentFailedEvent(
                        payment.getOrderId().toString(),
                        "Payment session expired"
                ));

                // Clear Redis initiation cache key to allow retry
                String cacheKey = REDIS_INITIATED_PREFIX + payment.getOrderNumber();
                redisTemplate.delete(cacheKey);

                log.info("Cleaned up expired payment={} for orderNumber={}", payment.getPaymentNumber(), payment.getOrderNumber());
            } catch (Exception e) {
                log.error("Failed to clean up expired payment={}", payment.getPaymentNumber(), e);
            }
        }
    }
}
