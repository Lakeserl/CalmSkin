package com.lakeserl.payment_service.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.lakeserl.payment_service.event.producer.PaymentEventProducer;
import com.lakeserl.payment_service.gateway.PaymentGateway;
import com.lakeserl.payment_service.gateway.PaymentGatewayFactory;
import com.lakeserl.payment_service.gateway.dto.WebhookVerifyResult;
import com.lakeserl.payment_service.models.entity.Payment;
import com.lakeserl.payment_service.models.entity.PaymentWebhook;
import com.lakeserl.payment_service.models.enums.PaymentMethod;
import com.lakeserl.payment_service.models.enums.PaymentStatus;
import com.lakeserl.payment_service.repository.PaymentRepository;
import com.lakeserl.payment_service.repository.PaymentWebhookRepository;

@ExtendWith(MockitoExtension.class)
class PaymentWebhookTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentWebhookRepository webhookRepository;

    @Mock
    private PaymentGatewayFactory gatewayFactory;

    @Mock
    private PaymentGateway paymentGateway;

    @Mock
    private PaymentEventProducer eventProducer;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    @Test
    void testProcessWebhook_InvalidSignature() {
        // Arrange
        Map<String, String> params = Map.of("vnp_TxnRef", "CS-12345");
        when(gatewayFactory.getGateway(PaymentMethod.VNPAY)).thenReturn(paymentGateway);
        
        WebhookVerifyResult verifyResult = WebhookVerifyResult.builder()
                .signatureValid(false)
                .success(false)
                .transactionRef("CS-12345")
                .build();
        when(paymentGateway.verifyWebhook(params)).thenReturn(verifyResult);

        // Act
        WebhookVerifyResult result = paymentService.processWebhook("VNPAY", params);

        // Assert
        assertFalse(result.signatureValid());
        ArgumentCaptor<PaymentWebhook> webhookCaptor = ArgumentCaptor.forClass(PaymentWebhook.class);
        verify(webhookRepository).save(webhookCaptor.capture());
        PaymentWebhook savedWebhook = webhookCaptor.getValue();
        assertFalse(savedWebhook.getSignatureValid());
        assertEquals("Invalid signature", savedWebhook.getErrorMessage());
        verifyNoInteractions(paymentRepository, eventProducer);
    }

    @Test
    void testProcessWebhook_PaymentNotFound() {
        // Arrange
        Map<String, String> params = Map.of("vnp_TxnRef", "CS-12345");
        when(gatewayFactory.getGateway(PaymentMethod.VNPAY)).thenReturn(paymentGateway);
        
        WebhookVerifyResult verifyResult = WebhookVerifyResult.builder()
                .signatureValid(true)
                .success(true)
                .transactionRef("CS-12345")
                .build();
        when(paymentGateway.verifyWebhook(params)).thenReturn(verifyResult);
        when(paymentRepository.findByOrderNumber("CS-12345")).thenReturn(Optional.empty());
        when(paymentRepository.findByPaymentNumber("CS-12345")).thenReturn(Optional.empty());

        // Act
        WebhookVerifyResult result = paymentService.processWebhook("VNPAY", params);

        // Assert
        assertTrue(result.signatureValid());
        ArgumentCaptor<PaymentWebhook> webhookCaptor = ArgumentCaptor.forClass(PaymentWebhook.class);
        verify(webhookRepository).save(webhookCaptor.capture());
        PaymentWebhook savedWebhook = webhookCaptor.getValue();
        assertTrue(savedWebhook.getSignatureValid());
        assertTrue(savedWebhook.getErrorMessage().contains("Payment not found"));
    }

    @Test
    void testProcessWebhook_AmountMismatch() {
        // Arrange
        Map<String, String> params = Map.of("vnp_TxnRef", "CS-12345");
        when(gatewayFactory.getGateway(PaymentMethod.VNPAY)).thenReturn(paymentGateway);
        
        WebhookVerifyResult verifyResult = WebhookVerifyResult.builder()
                .signatureValid(true)
                .success(true)
                .transactionRef("CS-12345")
                .amount(200000L) // Webhook says 200,000 VND
                .build();
        when(paymentGateway.verifyWebhook(params)).thenReturn(verifyResult);

        Payment payment = Payment.builder()
                .id(1L)
                .orderNumber("CS-12345")
                .amount(150000L) // DB expects 150,000 VND
                .status(PaymentStatus.PENDING)
                .build();
        when(paymentRepository.findByOrderNumber("CS-12345")).thenReturn(Optional.of(payment));

        // Act
        WebhookVerifyResult result = paymentService.processWebhook("VNPAY", params);

        // Assert
        assertTrue(result.signatureValid());
        ArgumentCaptor<PaymentWebhook> webhookCaptor = ArgumentCaptor.forClass(PaymentWebhook.class);
        verify(webhookRepository).save(webhookCaptor.capture());
        PaymentWebhook savedWebhook = webhookCaptor.getValue();
        assertEquals(1L, savedWebhook.getPaymentId());
        assertTrue(savedWebhook.getErrorMessage().contains("Amount mismatch"));
        assertEquals(PaymentStatus.PENDING, payment.getStatus());
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void testProcessWebhook_Success_StatusCompleted() {
        // Arrange
        Map<String, String> params = Map.of("vnp_TxnRef", "CS-12345");
        when(gatewayFactory.getGateway(PaymentMethod.VNPAY)).thenReturn(paymentGateway);
        
        WebhookVerifyResult verifyResult = WebhookVerifyResult.builder()
                .signatureValid(true)
                .success(true)
                .transactionRef("CS-12345")
                .transactionId("ZP12345")
                .amount(150000L)
                .build();
        when(paymentGateway.verifyWebhook(params)).thenReturn(verifyResult);

        Payment payment = Payment.builder()
                .id(1L)
                .orderId(999L)
                .orderNumber("CS-12345")
                .amount(150000L)
                .status(PaymentStatus.PENDING)
                .method(PaymentMethod.VNPAY)
                .build();
        when(paymentRepository.findByOrderNumber("CS-12345")).thenReturn(Optional.of(payment));

        // Act
        WebhookVerifyResult result = paymentService.processWebhook("VNPAY", params);

        // Assert
        assertTrue(result.signatureValid());
        assertTrue(result.success());
        assertEquals(PaymentStatus.COMPLETED, payment.getStatus());
        assertEquals("ZP12345", payment.getGatewayTransactionId());
        assertNotNull(payment.getPaidAt());

        verify(paymentRepository).save(payment);
        
        ArgumentCaptor<PaymentWebhook> webhookCaptor = ArgumentCaptor.forClass(PaymentWebhook.class);
        verify(webhookRepository).save(webhookCaptor.capture());
        PaymentWebhook savedWebhook = webhookCaptor.getValue();
        assertTrue(savedWebhook.getProcessed());
        assertNull(savedWebhook.getErrorMessage());

        verify(eventProducer).publish(eq("payment.completed"), eq("999"), any());
    }

    @Test
    void testProcessWebhook_DuplicateWebhookIgnored() {
        // Arrange
        Map<String, String> params = Map.of("vnp_TxnRef", "CS-12345");
        when(gatewayFactory.getGateway(PaymentMethod.VNPAY)).thenReturn(paymentGateway);
        
        WebhookVerifyResult verifyResult = WebhookVerifyResult.builder()
                .signatureValid(true)
                .success(true)
                .transactionRef("CS-12345")
                .amount(150000L)
                .build();
        when(paymentGateway.verifyWebhook(params)).thenReturn(verifyResult);

        Payment payment = Payment.builder()
                .id(1L)
                .orderNumber("CS-12345")
                .amount(150000L)
                .status(PaymentStatus.COMPLETED) // already completed!
                .build();
        when(paymentRepository.findByOrderNumber("CS-12345")).thenReturn(Optional.of(payment));

        // Act
        WebhookVerifyResult result = paymentService.processWebhook("VNPAY", params);

        // Assert
        assertTrue(result.signatureValid());
        verify(paymentRepository, never()).save(any(Payment.class));
        verifyNoInteractions(eventProducer);

        ArgumentCaptor<PaymentWebhook> webhookCaptor = ArgumentCaptor.forClass(PaymentWebhook.class);
        verify(webhookRepository).save(webhookCaptor.capture());
        PaymentWebhook savedWebhook = webhookCaptor.getValue();
        assertTrue(savedWebhook.getProcessed());
        assertEquals("Already processed previously", savedWebhook.getErrorMessage());
    }
}
