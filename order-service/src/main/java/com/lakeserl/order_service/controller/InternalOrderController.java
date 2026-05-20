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

@RestController
@RequestMapping("/internal/orders")
@RequiredArgsConstructor
public class InternalOrderController {

    private final OrderService orderService;

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
            @PathVariable Long userId,
            @RequestParam(required = false) String status,
            Pageable pageable) {
        
        OrderStatus orderStatus = null;
        if (status != null && !status.isBlank()) {
            orderStatus = OrderStatus.valueOf(status.toUpperCase());
        }

        Page<OrderSummaryDTO> response = orderService.getUserOrders(userId, orderStatus, pageable);
        return ApiResponse.ok(response);
    }
}
