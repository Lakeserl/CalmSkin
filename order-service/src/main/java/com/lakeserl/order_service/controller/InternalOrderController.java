package com.lakeserl.order_service.controller;

import com.lakeserl.order_service.dto.response.ApiResponse;
import com.lakeserl.order_service.dto.response.OrderDTO;
import com.lakeserl.order_service.dto.response.OrderSummaryDTO;
import com.lakeserl.order_service.enums.OrderStatus;
import com.lakeserl.order_service.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import com.lakeserl.order_service.dto.request.CreateOrderInternalRequest;

import java.util.UUID;

@RestController
@RequestMapping("/internal/orders")
@RequiredArgsConstructor
public class InternalOrderController {

    private final OrderService orderService;

    @PostMapping
    public ApiResponse<OrderDTO> createOrderInternal(@RequestBody CreateOrderInternalRequest request) {
        OrderDTO response = orderService.createOrder(request.userId(), request.orderRequest());
        return ApiResponse.ok("Order created internally", response);
    }

    @GetMapping("/{orderNumber}")
    public ApiResponse<OrderDTO> getOrderDetail(@PathVariable String orderNumber) {
        OrderDTO response = orderService.getOrderDetail(orderNumber);
        return ApiResponse.ok(response);
    }

    @PostMapping("/{orderNumber}/payment-completed")
    public ApiResponse<Void> paymentCompleted(
            @PathVariable String orderNumber,
            @RequestParam String transactionId,
            @RequestParam String method) {
        // Expose endpoint for payment service
        orderService.updateOrderStatusAdmin(orderNumber, "PAID", "Payment received successfully: transactionId=" + transactionId + " via " + method, "payment-service");
        orderService.updateOrderStatusAdmin(orderNumber, "PREPARING", "Auto-transitioned to preparing after payment", "system");
        return ApiResponse.ok("Payment confirmed internally", null);
    }

    @PostMapping("/{orderNumber}/payment-failed")
    public ApiResponse<Void> paymentFailed(
            @PathVariable String orderNumber,
            @RequestParam String reason) {
        orderService.cancelOrderSystem(orderNumber, "Payment failed: " + reason);
        return ApiResponse.ok("Payment failure recorded internally", null);
    }

    @GetMapping("/user/{userId}")
    public ApiResponse<Page<OrderSummaryDTO>> getUserOrdersInternal(
            @PathVariable UUID userId,
            @RequestParam(required = false) String status,
            Pageable pageable) {
        
        OrderStatus orderStatus = null;
        if (status != null && !status.isBlank()) {
            orderStatus = OrderStatus.valueOf(status.toUpperCase());
        }

        Page<OrderSummaryDTO> response = orderService.getUserOrders(userId, orderStatus, pageable);
        return ApiResponse.ok(response);
    }

    /**
     * Returns distinct productIds purchased by a user in DELIVERED orders within the past 365 days.
     * Used by product-service recommendation algorithm for exclusion + brand-affinity boost.
     */
    @GetMapping("/users/{userId}/purchased-product-ids")
    public ApiResponse<java.util.List<Long>> getPurchasedProductIds(@PathVariable UUID userId) {
        java.time.LocalDateTime since = java.time.LocalDateTime.now().minusDays(365);
        return ApiResponse.ok(orderService.getPurchasedProductIds(userId, since));
    }
}
