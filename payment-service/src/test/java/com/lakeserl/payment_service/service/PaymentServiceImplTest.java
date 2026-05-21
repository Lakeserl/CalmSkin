package com.lakeserl.payment_service.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import com.lakeserl.payment_service.config.properties.AppProperties;
import com.lakeserl.payment_service.event.payload.inbound.OrderConfirmedEvent;
import com.lakeserl.payment_service.event.payload.inbound.OrderCancelledEvent;
import com.lakeserl.payment_service.event.payload.outbound.PaymentCompletedEvent;
import com.lakeserl.payment_service.event.payload.outbound.PaymentFailedEvent;
import com.lakeserl.payment_service.event.producer.PaymentEventProducer;
import com.lakeserl.payment_service.exception.InvalidPaymentStateException;
import com.lakeserl.payment_service.exception.PaymentNotFoundException;
import com.lakeserl.payment_service.repository.RefundRepository;
import com.lakeserl.payment_service.gateway.PaymentGateway;
import com.lakeserl.payment_service.gateway.PaymentGatewayFactory;
import com.lakeserl.payment_service.gateway.dto.PaymentInitRequest;
import com.lakeserl.payment_service.gateway.dto.PaymentInitResult;
import com.lakeserl.payment_service.models.dto.request.PaymentInitiateRequest;
import com.lakeserl.payment_service.models.entity.Payment;
import com.lakeserl.payment_service.models.enums.PaymentMethod;
import com.lakeserl.payment_service.models.enums.PaymentStatus;
import com.lakeserl.payment_service.repository.PaymentRepository;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    private static final UUID TEST_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000456");

    @Mock
    private PaymentRepository paymentRepository;
    
    @Mock
    private RefundRepository refundRepository;

    @Mock
    private PaymentGatewayFactory gatewayFactory;

    @Mock
    private PaymentEventProducer eventProducer;

    @Mock
    private AppProperties appProperties;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private PaymentGateway paymentGateway;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    @BeforeEach
    void setUp() {
        lenient().when(appProperties.expiryMinutes()).thenReturn(15);
    }

    @Test
    void testProcessOrderConfirmed_NewPayment() {
        // Arrange
        OrderConfirmedEvent event = new OrderConfirmedEvent(
                "123", "CS-12345", TEST_USER_ID, BigDecimal.valueOf(150000), "VNPAY"
        );
        when(paymentRepository.existsByOrderId(123L)).thenReturn(false);

        // Act
        paymentService.processOrderConfirmed(event);

        // Assert
        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(paymentCaptor.capture());
        Payment saved = paymentCaptor.getValue();

        assertEquals(123L, saved.getOrderId());
        assertEquals("CS-12345", saved.getOrderNumber());
        assertEquals(TEST_USER_ID, saved.getUserId());
        assertEquals(150000L, saved.getAmount());
        assertEquals(PaymentMethod.VNPAY, saved.getMethod());
        assertEquals(PaymentStatus.PENDING, saved.getStatus());
        assertTrue(saved.getPaymentNumber().startsWith("PAY-"));
    }

    @Test
    void testProcessOrderConfirmed_AlreadyExists() {
        // Arrange
        OrderConfirmedEvent event = new OrderConfirmedEvent(
                "123", "CS-12345", TEST_USER_ID, BigDecimal.valueOf(150000), "VNPAY"
        );
        when(paymentRepository.existsByOrderId(123L)).thenReturn(true);

        // Act
        paymentService.processOrderConfirmed(event);

        // Assert
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void testInitiatePayment_PointsZeroAmount() {
        // Arrange
        PaymentInitiateRequest request = new PaymentInitiateRequest("CS-12345", "POINTS");
        Payment payment = Payment.builder()
                .orderId(123L)
                .orderNumber("CS-12345")
                .userId(TEST_USER_ID)
                .amount(0L)
                .method(PaymentMethod.POINTS)
                .status(PaymentStatus.PENDING)
                .build();

        when(paymentRepository.findByOrderNumber("CS-12345")).thenReturn(Optional.of(payment));

        // Act
        Payment result = paymentService.initiatePayment(request, "127.0.0.1");

        // Assert
        assertEquals(PaymentStatus.COMPLETED, result.getStatus());
        assertNotNull(result.getPaidAt());
        verify(paymentRepository).save(payment);
        verify(eventProducer).publish(eq("payment.completed"), eq("123"), any(PaymentCompletedEvent.class));
    }

    @Test
    void testInitiatePayment_CodMethod() {
        // Arrange
        PaymentInitiateRequest request = new PaymentInitiateRequest("CS-12345", "COD");
        Payment payment = Payment.builder()
                .orderId(123L)
                .orderNumber("CS-12345")
                .userId(TEST_USER_ID)
                .amount(150000L)
                .method(PaymentMethod.COD)
                .status(PaymentStatus.PENDING)
                .build();

        when(paymentRepository.findByOrderNumber("CS-12345")).thenReturn(Optional.of(payment));

        // Act
        Payment result = paymentService.initiatePayment(request, "127.0.0.1");

        // Assert
        assertEquals(PaymentStatus.PENDING, result.getStatus());
        verify(paymentRepository).save(payment);
        verifyNoInteractions(gatewayFactory, eventProducer);
    }

    @Test
    void testInitiatePayment_OnlineMethod_CachedUrl() {
        // Arrange
        PaymentInitiateRequest request = new PaymentInitiateRequest("CS-12345", "VNPAY");
        Payment payment = Payment.builder()
                .orderId(123L)
                .orderNumber("CS-12345")
                .userId(TEST_USER_ID)
                .amount(150000L)
                .method(PaymentMethod.VNPAY)
                .status(PaymentStatus.PENDING)
                .build();

        when(paymentRepository.findByOrderNumber("CS-12345")).thenReturn(Optional.of(payment));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("payment:initiated:CS-12345")).thenReturn("http://vnpay.vn/cached-payment-link");

        // Act
        Payment result = paymentService.initiatePayment(request, "127.0.0.1");

        // Assert
        assertEquals("http://vnpay.vn/cached-payment-link", result.getPaymentUrl());
        verifyNoInteractions(gatewayFactory);
    }

    @Test
    void testInitiatePayment_OnlineMethod_NewLink_Success() {
        // Arrange
        PaymentInitiateRequest request = new PaymentInitiateRequest("CS-12345", "VNPAY");
        Payment payment = Payment.builder()
                .orderId(123L)
                .orderNumber("CS-12345")
                .userId(TEST_USER_ID)
                .amount(150000L)
                .method(PaymentMethod.VNPAY)
                .status(PaymentStatus.PENDING)
                .build();

        when(paymentRepository.findByOrderNumber("CS-12345")).thenReturn(Optional.of(payment));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("payment:initiated:CS-12345")).thenReturn(null);
        
        when(gatewayFactory.getGateway(PaymentMethod.VNPAY)).thenReturn(paymentGateway);
        PaymentInitResult initResult = new PaymentInitResult("http://vnpay.vn/new-link", "TXN-123", true, null);
        when(paymentGateway.initiate(any(PaymentInitRequest.class))).thenReturn(initResult);

        // Act
        Payment result = paymentService.initiatePayment(request, "127.0.0.1");

        // Assert
        assertEquals("http://vnpay.vn/new-link", result.getPaymentUrl());
        assertEquals("TXN-123", result.getTransactionRef());
        assertNotNull(result.getExpiresAt());
        verify(paymentRepository).save(payment);
        verify(valueOperations).set(eq("payment:initiated:CS-12345"), eq("http://vnpay.vn/new-link"), any());
    }

    @Test
    void testInitiatePayment_OnlineMethod_NewLink_Failure() {
        // Arrange
        PaymentInitiateRequest request = new PaymentInitiateRequest("CS-12345", "VNPAY");
        Payment payment = Payment.builder()
                .orderId(123L)
                .orderNumber("CS-12345")
                .userId(TEST_USER_ID)
                .amount(150000L)
                .method(PaymentMethod.VNPAY)
                .status(PaymentStatus.PENDING)
                .build();

        when(paymentRepository.findByOrderNumber("CS-12345")).thenReturn(Optional.of(payment));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("payment:initiated:CS-12345")).thenReturn(null);
        
        when(gatewayFactory.getGateway(PaymentMethod.VNPAY)).thenReturn(paymentGateway);
        PaymentInitResult initResult = new PaymentInitResult(null, null, false, "Signature Error");
        when(paymentGateway.initiate(any(PaymentInitRequest.class))).thenReturn(initResult);

        // Act
        Payment result = paymentService.initiatePayment(request, "127.0.0.1");

        // Assert
        assertEquals(PaymentStatus.FAILED, result.getStatus());
        assertEquals("Signature Error", result.getFailureReason());
        verify(paymentRepository).save(payment);
        verify(eventProducer).publish(eq("payment.failed"), eq("123"), any(PaymentFailedEvent.class));
    }

    @Test
    void testConfirmCodPayment_Success() {
        // Arrange
        Payment payment = Payment.builder()
                .id(1L)
                .orderId(123L)
                .orderNumber("CS-12345")
                .userId(TEST_USER_ID)
                .amount(150000L)
                .method(PaymentMethod.COD)
                .status(PaymentStatus.PENDING)
                .build();
        when(paymentRepository.findByOrderNumber("CS-12345")).thenReturn(Optional.of(payment));

        // Act
        Payment result = paymentService.confirmCodPayment("CS-12345");

        // Assert
        assertEquals(PaymentStatus.COMPLETED, result.getStatus());
        assertNotNull(result.getPaidAt());
        verify(paymentRepository).save(payment);
        verify(eventProducer).publish(eq("payment.completed"), eq("123"), any(PaymentCompletedEvent.class));
    }

    @Test
    void testConfirmCodPayment_NotCod() {
        // Arrange
        Payment payment = Payment.builder()
                .id(1L)
                .orderId(123L)
                .orderNumber("CS-12345")
                .userId(TEST_USER_ID)
                .amount(150000L)
                .method(PaymentMethod.VNPAY) // VNPAY, not COD!
                .status(PaymentStatus.PENDING)
                .build();
        when(paymentRepository.findByOrderNumber("CS-12345")).thenReturn(Optional.of(payment));

        // Act & Assert
        assertThrows(InvalidPaymentStateException.class, () -> {
            paymentService.confirmCodPayment("CS-12345");
        });
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void testRefundPayment_CodAutoSuccess() {
        // Arrange
        Payment payment = Payment.builder()
                .id(1L)
                .orderId(123L)
                .orderNumber("CS-12345")
                .paymentNumber("PAY-12345")
                .userId(TEST_USER_ID)
                .amount(150000L)
                .refundedAmount(0L)
                .method(PaymentMethod.COD)
                .status(PaymentStatus.COMPLETED)
                .build();

        when(paymentRepository.findByPaymentNumber("PAY-12345")).thenReturn(Optional.of(payment));
        when(refundRepository.save(any(com.lakeserl.payment_service.models.entity.Refund.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // Act
        com.lakeserl.payment_service.models.entity.Refund result = paymentService.refundPayment("PAY-12345", 50000L, "Return product");

        // Assert
        assertEquals(com.lakeserl.payment_service.models.enums.RefundStatus.COMPLETED, result.getStatus());
        assertEquals(50000L, result.getAmount());
        assertEquals(50000L, payment.getRefundedAmount());
        verify(paymentRepository).save(payment);
        verify(eventProducer).publish(eq("payment.refunded"), eq("123"), any());
    }

    @Test
    void testRefundPayment_OnlineGatewaySuccess() {
        // Arrange
        Payment payment = Payment.builder()
                .id(1L)
                .orderId(123L)
                .orderNumber("CS-12345")
                .paymentNumber("PAY-12345")
                .gatewayTransactionId("GTXN-123")
                .userId(TEST_USER_ID)
                .amount(150000L)
                .refundedAmount(0L)
                .method(PaymentMethod.VNPAY)
                .status(PaymentStatus.COMPLETED)
                .build();

        when(paymentRepository.findByPaymentNumber("PAY-12345")).thenReturn(Optional.of(payment));
        when(refundRepository.save(any(com.lakeserl.payment_service.models.entity.Refund.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(gatewayFactory.getGateway(PaymentMethod.VNPAY)).thenReturn(paymentGateway);
        when(paymentGateway.refund(any())).thenReturn(new com.lakeserl.payment_service.gateway.dto.RefundResult(true, "GR-123", null));

        // Act
        com.lakeserl.payment_service.models.entity.Refund result = paymentService.refundPayment("PAY-12345", 150000L, "Cancel order");

        // Assert
        assertEquals(com.lakeserl.payment_service.models.enums.RefundStatus.COMPLETED, result.getStatus());
        assertEquals(150000L, payment.getRefundedAmount());
        assertEquals(PaymentStatus.REFUNDED, payment.getStatus());
        verify(paymentRepository).save(payment);
        verify(eventProducer).publish(eq("payment.refunded"), eq("123"), any());
    }

    @Test
    void testRefundPayment_GatewayFailure() {
        // Arrange
        Payment payment = Payment.builder()
                .id(1L)
                .orderId(123L)
                .orderNumber("CS-12345")
                .paymentNumber("PAY-12345")
                .gatewayTransactionId("GTXN-123")
                .userId(TEST_USER_ID)
                .amount(150000L)
                .refundedAmount(0L)
                .method(PaymentMethod.VNPAY)
                .status(PaymentStatus.COMPLETED)
                .build();

        when(paymentRepository.findByPaymentNumber("PAY-12345")).thenReturn(Optional.of(payment));
        when(refundRepository.save(any(com.lakeserl.payment_service.models.entity.Refund.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(gatewayFactory.getGateway(PaymentMethod.VNPAY)).thenReturn(paymentGateway);
        when(paymentGateway.refund(any())).thenReturn(new com.lakeserl.payment_service.gateway.dto.RefundResult(false, null, "Account limit exceeded"));

        // Act
        com.lakeserl.payment_service.models.entity.Refund result = paymentService.refundPayment("PAY-12345", 150000L, "Cancel order");

        // Assert
        assertEquals(com.lakeserl.payment_service.models.enums.RefundStatus.FAILED, result.getStatus());
        assertEquals("Account limit exceeded", result.getFailureReason());
        verify(paymentRepository, never()).save(payment);
        verify(eventProducer, never()).publish(any(), any(), any());
    }

    @Test
    void testRefundPayment_InvalidState() {
        // Arrange
        Payment payment = Payment.builder()
                .id(1L)
                .paymentNumber("PAY-12345")
                .status(PaymentStatus.FAILED) // Not completed!
                .build();

        when(paymentRepository.findByPaymentNumber("PAY-12345")).thenReturn(Optional.of(payment));

        // Act & Assert
        assertThrows(InvalidPaymentStateException.class, () -> {
            paymentService.refundPayment("PAY-12345", 50000L, "Return");
        });
    }

    @Test
    void testRefundPayment_AmountExceedsMax() {
        // Arrange
        Payment payment = Payment.builder()
                .id(1L)
                .paymentNumber("PAY-12345")
                .amount(150000L)
                .refundedAmount(100000L) // only 50000 left
                .status(PaymentStatus.COMPLETED)
                .build();

        when(paymentRepository.findByPaymentNumber("PAY-12345")).thenReturn(Optional.of(payment));

        // Act & Assert
        assertThrows(InvalidPaymentStateException.class, () -> {
            paymentService.refundPayment("PAY-12345", 60000L, "Return");
        });
    }

    @Test
    void testProcessOrderCancelled_CompletedPayment() {
        // Arrange
        OrderCancelledEvent event = new OrderCancelledEvent("123", "CS-12345", TEST_USER_ID, "User change mind");
        Payment payment = Payment.builder()
                .id(1L)
                .orderId(123L)
                .paymentNumber("PAY-12345")
                .amount(150000L)
                .refundedAmount(0L)
                .method(PaymentMethod.COD)
                .status(PaymentStatus.COMPLETED)
                .build();

        when(paymentRepository.findByOrderId(123L)).thenReturn(Optional.of(payment));
        when(paymentRepository.findByPaymentNumber("PAY-12345")).thenReturn(Optional.of(payment));
        when(refundRepository.save(any(com.lakeserl.payment_service.models.entity.Refund.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // Act
        paymentService.processOrderCancelled(event);

        // Assert
        assertEquals(PaymentStatus.REFUNDED, payment.getStatus());
        verify(paymentRepository, times(1)).save(payment);
    }

    @Test
    void testProcessOrderCancelled_PendingPayment() {
        // Arrange
        OrderCancelledEvent event = new OrderCancelledEvent("123", "CS-12345", TEST_USER_ID, "Timeout");
        Payment payment = Payment.builder()
                .id(1L)
                .orderId(123L)
                .status(PaymentStatus.PENDING)
                .build();

        when(paymentRepository.findByOrderId(123L)).thenReturn(Optional.of(payment));

        // Act
        paymentService.processOrderCancelled(event);

        // Assert
        assertEquals(PaymentStatus.FAILED, payment.getStatus());
        assertEquals("Order cancelled: Timeout", payment.getFailureReason());
        verify(paymentRepository).save(payment);
    }
}
