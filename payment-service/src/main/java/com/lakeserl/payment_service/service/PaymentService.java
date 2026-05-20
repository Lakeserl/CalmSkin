package com.lakeserl.payment_service.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.lakeserl.payment_service.event.payload.inbound.OrderConfirmedEvent;
import com.lakeserl.payment_service.models.dto.request.PaymentInitiateRequest;
import com.lakeserl.payment_service.models.dto.response.PaymentDTO;
import com.lakeserl.payment_service.models.entity.Payment;

public interface PaymentService {

    /** Process incoming OrderConfirmedEvent from Kafka. Creates a PENDING payment. */
    void processOrderConfirmed(OrderConfirmedEvent event);

    /** Initiate online gateway payment link or handle COD/POINTS logic. */
    Payment initiatePayment(PaymentInitiateRequest request, String ipAddress);

    /** Fetch payment details by its human-readable payment number. */
    Payment getPaymentByNumber(String paymentNumber);

    /** Fetch payments associated with a specific user. */
    Page<Payment> getPaymentsByUserId(Long userId, Pageable pageable);

    /** Process gateway webhooks (IPN) with signature verification, deduplication, and Outbox publishing. */
    com.lakeserl.payment_service.gateway.dto.WebhookVerifyResult processWebhook(String gatewayName, java.util.Map<String, String> params);

    /** Confirm a COD payment by orderNumber (ADMIN only). */
    Payment confirmCodPayment(String orderNumber);

    /** Refund a payment (ADMIN or Owner). */
    com.lakeserl.payment_service.models.entity.Refund refundPayment(String paymentNumber, Long amount, String reason);

    /** Process OrderCancelledEvent to auto-refund COMPLETED payments. */
    void processOrderCancelled(com.lakeserl.payment_service.event.payload.inbound.OrderCancelledEvent event);

    /** Get all payments for admin query. */
    Page<Payment> getAllPayments(Pageable pageable);
}
