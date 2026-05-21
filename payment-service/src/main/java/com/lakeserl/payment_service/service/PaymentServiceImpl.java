package com.lakeserl.payment_service.service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lakeserl.payment_service.config.properties.AppProperties;
import com.lakeserl.payment_service.event.payload.inbound.OrderConfirmedEvent;
import com.lakeserl.payment_service.event.payload.outbound.PaymentCompletedEvent;
import com.lakeserl.payment_service.event.payload.outbound.PaymentFailedEvent;
import com.lakeserl.payment_service.event.producer.PaymentEventProducer;
import com.lakeserl.payment_service.exception.DuplicatePaymentException;
import com.lakeserl.payment_service.exception.InvalidPaymentStateException;
import com.lakeserl.payment_service.exception.PaymentNotFoundException;
import com.lakeserl.payment_service.gateway.PaymentGateway;
import com.lakeserl.payment_service.gateway.PaymentGatewayFactory;
import com.lakeserl.payment_service.gateway.dto.PaymentInitRequest;
import com.lakeserl.payment_service.gateway.dto.PaymentInitResult;
import com.lakeserl.payment_service.models.dto.request.PaymentInitiateRequest;
import com.lakeserl.payment_service.event.payload.inbound.OrderCancelledEvent;
import com.lakeserl.payment_service.event.payload.outbound.PaymentRefundedEvent;
import com.lakeserl.payment_service.models.entity.Payment;
import com.lakeserl.payment_service.models.entity.PaymentWebhook;
import com.lakeserl.payment_service.models.entity.Refund;
import com.lakeserl.payment_service.models.enums.PaymentMethod;
import com.lakeserl.payment_service.models.enums.PaymentStatus;
import com.lakeserl.payment_service.models.enums.RefundMethod;
import com.lakeserl.payment_service.models.enums.RefundStatus;
import com.lakeserl.payment_service.repository.PaymentRepository;
import com.lakeserl.payment_service.repository.PaymentWebhookRepository;
import com.lakeserl.payment_service.repository.RefundRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentServiceImpl.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final String REDIS_INITIATED_PREFIX = "payment:initiated:";

    private final PaymentRepository paymentRepository;
    private final PaymentWebhookRepository webhookRepository;
    private final RefundRepository refundRepository;
    private final PaymentGatewayFactory gatewayFactory;
    private final AppProperties appProperties;
    private final StringRedisTemplate redisTemplate;
    private final PaymentEventProducer eventProducer;

    @Override
    @Transactional
    public void processOrderConfirmed(OrderConfirmedEvent event) {
        log.info("Processing OrderConfirmedEvent for orderId={}, orderNumber={}", event.orderId(), event.orderNumber());

        Long orderId = Long.parseLong(event.orderId());
        if (paymentRepository.existsByOrderId(orderId)) {
            log.warn("Payment for orderId={} already exists. Skipping creation.", orderId);
            return;
        }

        PaymentMethod method;
        try {
            method = PaymentMethod.valueOf(event.paymentMethod().toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            log.error("Invalid payment method: {} for orderId={}", event.paymentMethod(), orderId);
            return;
        }

        Payment payment = Payment.builder()
                .paymentNumber(generatePaymentNumber())
                .orderId(orderId)
                .orderNumber(event.orderNumber())
                .userId(event.userId())
                .amount(event.totalAmount().longValue())
                .method(method)
                .status(PaymentStatus.PENDING)
                .refundedAmount(0L)
                .build();

        paymentRepository.save(payment);
        log.info("Created PENDING payment={} for orderNumber={}", payment.getPaymentNumber(), payment.getOrderNumber());
    }

    @Override
    @Transactional
    public Payment initiatePayment(PaymentInitiateRequest request, String ipAddress) {
        log.info("Initiating payment for orderNumber={}, method={}", request.orderNumber(), request.paymentMethod());

        Payment payment = paymentRepository.findByOrderNumber(request.orderNumber())
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found for order number " + request.orderNumber()));

        if (payment.getStatus() == PaymentStatus.COMPLETED) {
            throw new InvalidPaymentStateException("Payment is already completed");
        }

        PaymentMethod requestMethod;
        try {
            requestMethod = PaymentMethod.valueOf(request.paymentMethod().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidPaymentStateException("Invalid payment method: " + request.paymentMethod());
        }

        // Keep method in sync if changed by user at checkout redirect
        if (payment.getMethod() != requestMethod) {
            payment.setMethod(requestMethod);
        }

        // 1. POINTS Integration (Amount = 0 completes immediately)
        if (requestMethod == PaymentMethod.POINTS && payment.getAmount() == 0) {
            payment.setStatus(PaymentStatus.COMPLETED);
            payment.setPaidAt(LocalDateTime.now());
            paymentRepository.save(payment);

            log.info("Completed POINTS payment={} immediately since amount=0", payment.getPaymentNumber());
            eventProducer.publish("payment.completed", payment.getOrderId().toString(), new PaymentCompletedEvent(
                    payment.getOrderId().toString(),
                    "PTS-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
                    BigDecimal.valueOf(payment.getAmount()),
                    requestMethod.name()
            ));
            return payment;
        }

        // 2. COD Integration (PENDING but no payment link needed)
        if (requestMethod == PaymentMethod.COD) {
            payment.setStatus(PaymentStatus.PENDING);
            paymentRepository.save(payment);
            log.info("Initiated COD payment={} successfully", payment.getPaymentNumber());
            return payment;
        }

        // 3. Online Payments Deduplication & Integration
        String cacheKey = REDIS_INITIATED_PREFIX + payment.getOrderNumber();
        String cachedUrl = redisTemplate.opsForValue().get(cacheKey);
        if (cachedUrl != null) {
            log.info("Found cached payment URL for orderNumber={}", payment.getOrderNumber());
            payment.setPaymentUrl(cachedUrl);
            return payment;
        }

        PaymentGateway gateway = gatewayFactory.getGateway(requestMethod);
        PaymentInitRequest initRequest = PaymentInitRequest.builder()
                .orderNumber(payment.getOrderNumber())
                .amount(payment.getAmount())
                .ipAddress(ipAddress)
                .orderInfo("Payment for order " + payment.getOrderNumber())
                .build();

        PaymentInitResult initResult = gateway.initiate(initRequest);
        if (!initResult.success()) {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason(initResult.errorMessage());
            paymentRepository.save(payment);

            log.error("Payment initiation failed for orderNumber={}: {}", payment.getOrderNumber(), initResult.errorMessage());
            eventProducer.publish("payment.failed", payment.getOrderId().toString(), new PaymentFailedEvent(
                    payment.getOrderId().toString(),
                    initResult.errorMessage()
            ));
            return payment;
        }

        payment.setPaymentUrl(initResult.paymentUrl());
        payment.setTransactionRef(initResult.transactionRef());
        
        int expiryMinutes = appProperties.expiryMinutes();
        payment.setExpiresAt(LocalDateTime.now().plusMinutes(expiryMinutes));
        paymentRepository.save(payment);

        // Cache payment URL for 15 minutes to prevent duplicate creations
        redisTemplate.opsForValue().set(cacheKey, initResult.paymentUrl(), Duration.ofMinutes(expiryMinutes));
        log.info("Cached payment URL for orderNumber={} and saved to DB", payment.getOrderNumber());

        return payment;
    }

    @Override
    @Transactional(readOnly = true)
    public Payment getPaymentByNumber(String paymentNumber) {
        return paymentRepository.findByPaymentNumber(paymentNumber)
                .orElseThrow(() -> new PaymentNotFoundException("Payment with number " + paymentNumber + " not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Payment> getPaymentsByUserId(UUID userId, Pageable pageable) {
        return paymentRepository.findByUserId(userId, pageable);
    }

    @Override
    @Transactional
    public com.lakeserl.payment_service.gateway.dto.WebhookVerifyResult processWebhook(String gatewayName, java.util.Map<String, String> params) {
        log.info("Received IPN callback webhook for gateway={}, params={}", gatewayName, params);

        PaymentMethod method;
        try {
            method = PaymentMethod.valueOf(gatewayName.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.error("Unsupported gateway for webhook: {}", gatewayName);
            throw new IllegalArgumentException("Unsupported gateway: " + gatewayName);
        }

        PaymentGateway gateway = gatewayFactory.getGateway(method);
        com.lakeserl.payment_service.gateway.dto.WebhookVerifyResult verifyResult = gateway.verifyWebhook(params);

        // Prepare raw string of params for database auditing
        String rawParams = params.toString();

        PaymentWebhook webhook = PaymentWebhook.builder()
                .gateway(gatewayName)
                .rawParams(rawParams)
                .signatureValid(verifyResult.signatureValid())
                .processed(false)
                .build();

        if (!verifyResult.signatureValid()) {
            webhook.setErrorMessage("Invalid signature");
            webhookRepository.save(webhook);
            log.warn("Webhook signature validation failed for gateway={}", gatewayName);
            return verifyResult;
        }

        // If signature is valid, try to match payment by transactionRef
        String transactionRef = verifyResult.transactionRef();
        if (transactionRef == null || transactionRef.isBlank()) {
            webhook.setErrorMessage("Transaction ref is missing in verifyResult");
            webhookRepository.save(webhook);
            log.warn("Missing transaction ref in webhook for gateway={}", gatewayName);
            return verifyResult;
        }

        Payment payment = paymentRepository.findByOrderNumber(transactionRef)
                .orElse(null);

        if (payment == null) {
            // VNPay sometimes sends vnp_TxnRef, but could be payment number or order ID.
            // Let's also check by orderId if transactionRef is a number, or paymentNumber.
            payment = paymentRepository.findByPaymentNumber(transactionRef).orElse(null);
            if (payment == null) {
                try {
                    Long orderId = Long.parseLong(transactionRef);
                    payment = paymentRepository.findByOrderId(orderId).orElse(null);
                } catch (NumberFormatException ignored) {}
            }
        }

        if (payment == null) {
            webhook.setErrorMessage("Payment not found for transactionRef: " + transactionRef);
            webhookRepository.save(webhook);
            log.warn("No payment matched transactionRef={} for gateway={}", transactionRef, gatewayName);
            return verifyResult;
        }

        webhook.setPaymentId(payment.getId());

        // Validate amount (verifyResult.amount() vs payment.getAmount())
        if (verifyResult.amount() != null && !verifyResult.amount().equals(payment.getAmount())) {
            webhook.setErrorMessage(String.format("Amount mismatch. Expected: %d, Received: %d",
                    payment.getAmount(), verifyResult.amount()));
            webhookRepository.save(webhook);
            log.warn("Amount mismatch for paymentId={}, expected={}, received={}",
                    payment.getId(), payment.getAmount(), verifyResult.amount());
            return verifyResult;
        }

        // Check if already processed (completed or failed)
        if (payment.getStatus() == PaymentStatus.COMPLETED || payment.getStatus() == PaymentStatus.FAILED) {
            webhook.setProcessed(true);
            webhook.setErrorMessage("Already processed previously");
            webhookRepository.save(webhook);
            log.info("PaymentId={} was already processed to status={}, skipping duplicate IPN",
                    payment.getId(), payment.getStatus());
            return verifyResult;
        }

        // Process status update
        if (verifyResult.success()) {
            payment.setStatus(PaymentStatus.COMPLETED);
            payment.setGatewayTransactionId(verifyResult.transactionId());
            payment.setPaidAt(LocalDateTime.now());
            paymentRepository.save(payment);

            webhook.setProcessed(true);
            webhookRepository.save(webhook);

            log.info("PaymentId={} marked as COMPLETED via IPN. Publishing event...", payment.getId());
            eventProducer.publish("payment.completed", payment.getOrderId().toString(), new PaymentCompletedEvent(
                    payment.getOrderId().toString(),
                    verifyResult.transactionId() != null ? verifyResult.transactionId() : payment.getPaymentNumber(),
                    BigDecimal.valueOf(payment.getAmount()),
                    payment.getMethod().name()
            ));
        } else {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason("IPN reported failure / transaction unsuccessful");
            paymentRepository.save(payment);

            webhook.setProcessed(true);
            webhookRepository.save(webhook);

            log.info("PaymentId={} marked as FAILED via IPN. Publishing event...", payment.getId());
            eventProducer.publish("payment.failed", payment.getOrderId().toString(), new PaymentFailedEvent(
                    payment.getOrderId().toString(),
                    "Transaction unsuccessful at gateway"
            ));
        }

        return verifyResult;
    }

    @Override
    @Transactional
    public Payment confirmCodPayment(String orderNumber) {
        log.info("Confirming COD payment for orderNumber={}", orderNumber);

        Payment payment = paymentRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found for order number " + orderNumber));

        if (payment.getMethod() != PaymentMethod.COD) {
            throw new InvalidPaymentStateException("Payment method is not COD");
        }

        if (payment.getStatus() == PaymentStatus.COMPLETED) {
            throw new InvalidPaymentStateException("COD payment is already completed");
        }

        payment.setStatus(PaymentStatus.COMPLETED);
        payment.setPaidAt(LocalDateTime.now());
        paymentRepository.save(payment);

        log.info("COD payment={} confirmed successfully by ADMIN", payment.getPaymentNumber());

        eventProducer.publish("payment.completed", payment.getOrderId().toString(), new PaymentCompletedEvent(
                payment.getOrderId().toString(),
                payment.getPaymentNumber(),
                BigDecimal.valueOf(payment.getAmount()),
                payment.getMethod().name()
        ));

        return payment;
    }

    @Override
    @Transactional
    public Refund refundPayment(String paymentNumber, Long amount, String reason) {
        log.info("Requesting refund for paymentNumber={}, amount={}, reason={}", paymentNumber, amount, reason);

        Payment payment = paymentRepository.findByPaymentNumber(paymentNumber)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found for payment number " + paymentNumber));

        if (payment.getStatus() != PaymentStatus.COMPLETED) {
            throw new InvalidPaymentStateException("Cannot refund a payment with status " + payment.getStatus());
        }

        long maxRefundable = payment.getAmount() - (payment.getRefundedAmount() != null ? payment.getRefundedAmount() : 0L);
        if (amount > maxRefundable) {
            throw new InvalidPaymentStateException(String.format("Requested refund amount %d exceeds max refundable amount %d", amount, maxRefundable));
        }

        // Generate refund number
        String dateStr = LocalDateTime.now().format(DATE_FORMATTER);
        String randCode = org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric(8).toUpperCase();
        String refundNumber = "REF-" + dateStr + "-" + randCode;

        // Determine refund method
        RefundMethod refundMethod = RefundMethod.ORIGINAL;
        if (payment.getMethod() == PaymentMethod.POINTS) {
            refundMethod = RefundMethod.POINTS;
        }

        Refund refund = Refund.builder()
                .refundNumber(refundNumber)
                .paymentId(payment.getId())
                .orderId(payment.getOrderId())
                .amount(amount)
                .reason(reason)
                .refundMethod(refundMethod)
                .status(RefundStatus.PENDING)
                .build();

        refund = refundRepository.save(refund);

        // Call Gateway
        if (payment.getMethod() == PaymentMethod.COD || payment.getMethod() == PaymentMethod.POINTS) {
            // Auto-success for COD / POINTS
            refund.setStatus(RefundStatus.COMPLETED);
            refund.setProcessedAt(LocalDateTime.now());
            refundRepository.save(refund);

            payment.setRefundedAmount((payment.getRefundedAmount() != null ? payment.getRefundedAmount() : 0L) + amount);
            if (payment.getRefundedAmount().equals(payment.getAmount())) {
                payment.setStatus(PaymentStatus.REFUNDED);
            }
            paymentRepository.save(payment);

            log.info("COD/POINTS refund={} completed automatically", refundNumber);

            eventProducer.publish("payment.refunded", payment.getOrderId().toString(), new PaymentRefundedEvent(
                    payment.getOrderId().toString(),
                    refund.getRefundNumber(),
                    BigDecimal.valueOf(refund.getAmount()),
                    refund.getReason()
            ));
        } else {
            // Online Gateway
            PaymentGateway gateway = gatewayFactory.getGateway(payment.getMethod());
            com.lakeserl.payment_service.gateway.dto.RefundGatewayRequest refundRequest = com.lakeserl.payment_service.gateway.dto.RefundGatewayRequest.builder()
                    .transactionId(payment.getGatewayTransactionId())
                    .amount(amount)
                    .refundRef(refundNumber)
                    .reason(reason)
                    .ipAddress("127.0.0.1")
                    .transactionDate(payment.getPaidAt() != null ? payment.getPaidAt().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) : null)
                    .build();

            com.lakeserl.payment_service.gateway.dto.RefundResult gatewayResult = gateway.refund(refundRequest);

            if (gatewayResult.success()) {
                refund.setStatus(RefundStatus.COMPLETED);
                refund.setGatewayRefundId(gatewayResult.gatewayRefundId());
                refund.setProcessedAt(LocalDateTime.now());
                refundRepository.save(refund);

                payment.setRefundedAmount((payment.getRefundedAmount() != null ? payment.getRefundedAmount() : 0L) + amount);
                if (payment.getRefundedAmount().equals(payment.getAmount())) {
                    payment.setStatus(PaymentStatus.REFUNDED);
                }
                paymentRepository.save(payment);

                log.info("Refund={} completed successfully via gateway", refundNumber);

                eventProducer.publish("payment.refunded", payment.getOrderId().toString(), new PaymentRefundedEvent(
                        payment.getOrderId().toString(),
                        refund.getRefundNumber(),
                        BigDecimal.valueOf(refund.getAmount()),
                        refund.getReason()
                ));
            } else {
                refund.setStatus(RefundStatus.FAILED);
                refund.setFailureReason(gatewayResult.errorMessage());
                refundRepository.save(refund);

                log.error("Refund={} failed at gateway: {}", refundNumber, gatewayResult.errorMessage());
            }
        }

        return refund;
    }

    @Override
    @Transactional
    public void processOrderCancelled(OrderCancelledEvent event) {
        log.info("Processing OrderCancelledEvent for orderId={}, orderNumber={}, reason={}", event.orderId(), event.orderNumber(), event.reason());

        Long orderId = Long.parseLong(event.orderId());
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElse(null);

        if (payment == null) {
            log.warn("No payment found for cancelled orderId={}. Nothing to refund.", orderId);
            return;
        }

        if (payment.getStatus() != PaymentStatus.COMPLETED) {
            log.info("Payment for cancelled orderId={} has status={}, not COMPLETED. Marking payment as FAILED/CANCELLED and skipping refund.", orderId, payment.getStatus());
            if (payment.getStatus() == PaymentStatus.PENDING) {
                payment.setStatus(PaymentStatus.FAILED);
                payment.setFailureReason("Order cancelled: " + event.reason());
                paymentRepository.save(payment);
            }
            return;
        }

        long refundableAmount = payment.getAmount() - (payment.getRefundedAmount() != null ? payment.getRefundedAmount() : 0L);
        if (refundableAmount <= 0) {
            log.info("Payment for cancelled orderId={} has already been fully refunded.", orderId);
            return;
        }

        try {
            refundPayment(payment.getPaymentNumber(), refundableAmount, "Order cancelled: " + event.reason());
            log.info("Auto-refunded {} for cancelled orderId={}", refundableAmount, orderId);
        } catch (Exception e) {
            log.error("Failed to auto-refund for cancelled orderId={}", orderId, e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Payment> getAllPayments(Pageable pageable) {
        return paymentRepository.findAll(pageable);
    }

    private String generatePaymentNumber() {
        String dateStr = LocalDateTime.now().format(DATE_FORMATTER);
        String randCode = RandomStringUtils.randomAlphanumeric(8).toUpperCase();
        return "PAY-" + dateStr + "-" + randCode;
    }
}
