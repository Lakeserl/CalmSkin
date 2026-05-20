package com.lakeserl.order_service.controller;

import com.lakeserl.order_service.dto.request.CancelOrderRequest;
import com.lakeserl.order_service.dto.request.CreateOrderRequest;
import com.lakeserl.order_service.dto.request.ReturnOrderRequest;
import com.lakeserl.order_service.dto.response.ApiResponse;
import com.lakeserl.order_service.dto.response.OrderDTO;
import com.lakeserl.order_service.dto.response.OrderSummaryDTO;
import com.lakeserl.order_service.enums.OrderStatus;
import com.lakeserl.order_service.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ApiResponse<OrderDTO> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        Long userId = getCurrentUserId();
        OrderDTO response = orderService.createOrder(userId, request);
        return ApiResponse.ok("Order placed successfully", response);
    }

    @GetMapping("/{orderNumber}")
    public ApiResponse<OrderDTO> getOrderDetail(@PathVariable String orderNumber) {
        Long userId = getCurrentUserId();
        OrderDTO response = orderService.getOrderDetail(orderNumber, userId);
        return ApiResponse.ok(response);
    }

    @GetMapping
    public ApiResponse<Page<OrderSummaryDTO>> getUserOrders(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {
        
        Long userId = getCurrentUserId();
        OrderStatus orderStatus = null;
        if (status != null && !status.isBlank()) {
            orderStatus = OrderStatus.valueOf(status.toUpperCase());
        }

        String[] sortParams = sort.split(",");
        Sort sortObj = Sort.by(Sort.Direction.fromString(sortParams[1]), sortParams[0]);
        Pageable pageable = PageRequest.of(page, size, sortObj);

        Page<OrderSummaryDTO> response = orderService.getUserOrders(userId, orderStatus, pageable);
        
        ApiResponse.PageInfo pageInfo = ApiResponse.PageInfo.builder()
                .currentPage(response.getNumber())
                .pageSize(response.getSize())
                .totalElements(response.getTotalElements())
                .totalPages(response.getTotalPages())
                .hasNext(response.hasNext())
                .hasPrevious(response.hasPrevious())
                .build();

        return ApiResponse.ok("User orders fetched successfully", response, pageInfo);
    }

    @PostMapping("/{orderNumber}/cancel")
    public ApiResponse<Void> cancelOrder(
            @PathVariable String orderNumber,
            @Valid @RequestBody CancelOrderRequest request) {
        Long userId = getCurrentUserId();
        orderService.cancelOrder(orderNumber, userId, request.reason());
        return ApiResponse.ok("Order cancelled successfully", null);
    }

    @PostMapping("/{orderNumber}/return")
    public ApiResponse<Void> requestReturn(
            @PathVariable String orderNumber,
            @Valid @RequestBody ReturnOrderRequest request) {
        Long userId = getCurrentUserId();
        orderService.requestReturn(orderNumber, userId, request);
        return ApiResponse.ok("Return request submitted successfully", null);
    }

    private Long getCurrentUserId() {
        String principal = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return Long.parseLong(principal);
    }
}
