package com.lakeserl.payment_service.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.lakeserl.payment_service.models.entity.PaymentWebhook;

@Repository
public interface PaymentWebhookRepository extends JpaRepository<PaymentWebhook, Long> {

    List<PaymentWebhook> findByPaymentIdOrderByCreatedAtDesc(Long paymentId);
}
