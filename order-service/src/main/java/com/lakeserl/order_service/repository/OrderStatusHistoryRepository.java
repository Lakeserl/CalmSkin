package com.lakeserl.order_service.repository;

import com.lakeserl.order_service.entity.OrderStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderStatusHistoryRepository extends JpaRepository<OrderStatusHistory, Long> {
    List<OrderStatusHistory> findByOrderIdOrderByCreatedAtAsc(Long orderId);
    
    List<OrderStatusHistory> findByOrderOrderNumberOrderByCreatedAtAsc(String orderNumber);
}
