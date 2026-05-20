package com.lakeserl.payment_service.scheduler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;

import com.lakeserl.payment_service.event.producer.PaymentEventProducer;
import com.lakeserl.payment_service.models.entity.Payment;
import com.lakeserl.payment_service.models.enums.PaymentStatus;
import com.lakeserl.payment_service.repository.PaymentRepository;

@ExtendWith(MockitoExtension.class)
class PaymentExpirySchedulerTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentEventProducer eventProducer;

    @Mock
    private StringRedisTemplate redisTemplate;

    @InjectMocks
    private PaymentExpiryScheduler expiryScheduler;

    @Test
    void testCleanExpiredPayments_Success() {
        // Arrange
        Payment payment = Payment.builder()
                .id(1L)
                .orderId(123L)
                .orderNumber("CS-12345")
                .paymentNumber("PAY-20260520-XXXX")
                .amount(150000L)
                .status(PaymentStatus.PENDING)
                .expiresAt(LocalDateTime.now().minusMinutes(1))
                .build();

        when(paymentRepository.findByStatusAndExpiresAtBefore(eq(PaymentStatus.PENDING), any(LocalDateTime.class)))
                .thenReturn(List.of(payment));

        // Act
        expiryScheduler.cleanExpiredPayments();

        // Assert
        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(captor.capture());
        Payment saved = captor.getValue();

        assertEquals(PaymentStatus.FAILED, saved.getStatus());
        assertEquals("Payment session expired", saved.getFailureReason());

        verify(eventProducer).publish(eq("payment.failed"), eq("123"), any());
        verify(redisTemplate).delete("payment:initiated:CS-12345");
    }

    @Test
    void testCleanExpiredPayments_NoneFound() {
        // Arrange
        when(paymentRepository.findByStatusAndExpiresAtBefore(eq(PaymentStatus.PENDING), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

        // Act
        expiryScheduler.cleanExpiredPayments();

        // Assert
        verifyNoInteractions(eventProducer, redisTemplate);
        verify(paymentRepository, never()).save(any());
    }
}
