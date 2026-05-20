package com.lakeserl.order_service.repository;

import com.lakeserl.order_service.entity.OrderPaymentInfo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrderPaymentInfoRepository extends JpaRepository<OrderPaymentInfo, Long> {
    Optional<OrderPaymentInfo> findByOrderId(Long orderId);
}
