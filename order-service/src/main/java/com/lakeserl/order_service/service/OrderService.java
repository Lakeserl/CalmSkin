package com.lakeserl.order_service.service;

import com.lakeserl.order_service.dto.request.CreateOrderRequest;
import com.lakeserl.order_service.dto.request.ReturnOrderRequest;
import com.lakeserl.order_service.dto.response.OrderDTO;
import com.lakeserl.order_service.dto.response.OrderStatsDTO;
import com.lakeserl.order_service.dto.response.OrderSummaryDTO;
import com.lakeserl.order_service.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;

public interface OrderService {
    OrderDTO createOrder(Long userId, CreateOrderRequest request);
    
    OrderDTO getOrderDetail(String orderNumber);
    
    OrderDTO getOrderDetail(String orderNumber, Long userId);
    
    Page<OrderSummaryDTO> getUserOrders(Long userId, OrderStatus status, Pageable pageable);
    
    Page<OrderSummaryDTO> getAllOrdersAdmin(OrderStatus status, Long userId, String orderNumber, LocalDateTime fromDate, LocalDateTime toDate, Pageable pageable);
    
    void cancelOrder(String orderNumber, Long userId, String reason);
    
    void cancelOrderSystem(String orderNumber, String reason);
    
    void updateOrderStatusAdmin(String orderNumber, String status, String note, String adminUsername);
    
    void requestReturn(String orderNumber, Long userId, ReturnOrderRequest request);
    
    void confirmReturnAdmin(String orderNumber, String adminUsername);
    
    OrderStatsDTO getOrderStatsAdmin();
    
    String exportOrdersCsv(OrderStatus status, Long userId, String orderNumber, LocalDateTime fromDate, LocalDateTime toDate);
}
