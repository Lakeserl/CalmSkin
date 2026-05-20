package com.lakeserl.payment_service.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.lakeserl.payment_service.models.entity.Refund;
import com.lakeserl.payment_service.models.enums.RefundStatus;

@Repository
public interface RefundRepository extends JpaRepository<Refund, Long> {

    List<Refund> findByStatus(RefundStatus status);

    List<Refund> findByPaymentId(Long paymentId);

    List<Refund> findByOrderId(Long orderId);
}
