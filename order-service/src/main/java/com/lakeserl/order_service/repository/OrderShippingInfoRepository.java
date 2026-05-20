package com.lakeserl.order_service.repository;

import com.lakeserl.order_service.entity.OrderShippingInfo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrderShippingInfoRepository extends JpaRepository<OrderShippingInfo, Long> {
    Optional<OrderShippingInfo> findByOrderId(Long orderId);
}
