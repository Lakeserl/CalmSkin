package com.lakeserl.payment_service.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.lakeserl.payment_service.models.entity.Payment;
import com.lakeserl.payment_service.models.enums.PaymentStatus;

@Repository
public interface PaymentRepository
        extends JpaRepository<Payment, Long>, JpaSpecificationExecutor<Payment> {

    Optional<Payment> findByPaymentNumber(String paymentNumber);

    Optional<Payment> findByOrderId(Long orderId);

    Optional<Payment> findByOrderNumber(String orderNumber);

    boolean existsByOrderId(Long orderId);

    Optional<Payment> findByGatewayTransactionId(String gatewayTransactionId);

    Page<Payment> findByUserId(UUID userId, Pageable pageable);

    /** Online payments still PENDING past their expiry deadline. */
    List<Payment> findByStatusAndExpiresAtBefore(PaymentStatus status, LocalDateTime cutoff);
}
