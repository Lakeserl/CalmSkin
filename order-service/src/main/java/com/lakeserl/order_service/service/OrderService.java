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
import java.util.UUID;

public interface OrderService {
    OrderDTO createOrder(UUID userId, CreateOrderRequest request);

    OrderDTO getOrderDetail(String orderNumber);

    OrderDTO getOrderDetail(String orderNumber, UUID userId);

    Page<OrderSummaryDTO> getUserOrders(UUID userId, OrderStatus status, Pageable pageable);

    Page<OrderSummaryDTO> getAllOrdersAdmin(OrderStatus status, UUID userId, String orderNumber, LocalDateTime fromDate, LocalDateTime toDate, Pageable pageable);

    void cancelOrder(String orderNumber, UUID userId, String reason);

    void cancelOrderSystem(String orderNumber, String reason);

    void updateOrderStatusAdmin(String orderNumber, String status, String note, String adminUsername);

    void requestReturn(String orderNumber, UUID userId, ReturnOrderRequest request);

    void confirmReturnAdmin(String orderNumber, String adminUsername);

    OrderStatsDTO getOrderStatsAdmin();

    String exportOrdersCsv(OrderStatus status, UUID userId, String orderNumber, LocalDateTime fromDate, LocalDateTime toDate);

    /** Returns distinct productIds purchased by userId in DELIVERED orders since the given date. */
    java.util.List<Long> getPurchasedProductIds(UUID userId, LocalDateTime since);
}
