package com.lakeserl.order_service.service;

import com.lakeserl.order_service.entity.Order;
import com.lakeserl.order_service.enums.OrderStatus;

public interface OrderStatusService {
    void transitionTo(Order order, OrderStatus nextStatus, String changedBy, String reason, String metadata);
    
    void transitionTo(Order order, OrderStatus nextStatus, String changedBy, String reason);
    
    void validateTransition(OrderStatus current, OrderStatus next);
}
