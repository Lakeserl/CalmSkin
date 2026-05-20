package com.lakeserl.payment_service.models.entity;

import java.time.LocalDateTime;

import com.lakeserl.payment_service.models.enums.PaymentMethod;
import com.lakeserl.payment_service.models.enums.PaymentStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A payment record for a single order. One payment per order.
 * All monetary amounts are stored as BIGINT (VND) - never floating point.
 */
@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Human-readable unique reference, e.g. PAY-YYYYMMDD-XXXXXXXX. */
    @Column(name = "payment_number", nullable = false, unique = true, length = 30)
    private String paymentNumber;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    /** The order's human-readable number, received from the order.confirmed event. */
    @Column(name = "order_number", nullable = false, length = 40)
    private String orderNumber;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** Amount in VND (BIGINT). */
    @Column(name = "amount", nullable = false)
    private Long amount;

    /** Total amount already refunded. Used to determine PARTIALLY_REFUNDED vs REFUNDED. */
    @Column(name = "refunded_amount", nullable = false)
    @Builder.Default
    private Long refundedAmount = 0L;

    @Enumerated(EnumType.STRING)
    @Column(name = "method", nullable = false, length = 20)
    private PaymentMethod method;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PaymentStatus status;

    /** Reference we send TO the gateway (vnp_TxnRef / Momo orderId). */
    @Column(name = "transaction_ref", length = 100)
    private String transactionRef;

    /** Transaction id returned BY the gateway (vnp_TransactionNo / momo transId). Idempotency key for webhooks. */
    @Column(name = "gateway_transaction_id", length = 100)
    private String gatewayTransactionId;

    /** Raw gateway response payload for audit. */
    @Column(name = "gateway_response", columnDefinition = "TEXT")
    private String gatewayResponse;

    @Column(name = "payment_url", columnDefinition = "TEXT")
    private String paymentUrl;

    @Column(name = "failure_reason", length = 255)
    private String failureReason;

    /** When an online payment expires if not paid. Null for COD/POINTS. */
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (status == null) {
            status = PaymentStatus.PENDING;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
