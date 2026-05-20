package com.lakeserl.payment_service.models.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Raw inbound webhook from a payment gateway, stored before any processing
 * so every callback is auditable and replayable.
 */
@Entity
@Table(name = "payment_webhooks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentWebhook {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Nullable: the webhook may arrive before its payment can be matched. */
    @Column(name = "payment_id")
    private Long paymentId;

    @Column(name = "gateway", nullable = false, length = 20)
    private String gateway;

    /** Raw query params / request body of the webhook. */
    @Column(name = "raw_params", nullable = false, columnDefinition = "TEXT")
    private String rawParams;

    @Column(name = "signature_valid", nullable = false)
    private Boolean signatureValid;

    @Column(name = "processed", nullable = false)
    private Boolean processed;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (signatureValid == null) {
            signatureValid = false;
        }
        if (processed == null) {
            processed = false;
        }
    }
}
