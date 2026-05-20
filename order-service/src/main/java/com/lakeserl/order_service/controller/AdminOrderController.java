package com.lakeserl.order_service.controller;

import com.lakeserl.order_service.dto.request.CancelOrderRequest;
import com.lakeserl.order_service.dto.request.UpdateOrderStatusRequest;
import com.lakeserl.order_service.dto.response.ApiResponse;
import com.lakeserl.order_service.dto.response.OrderDTO;
import com.lakeserl.order_service.dto.response.OrderStatsDTO;
import com.lakeserl.order_service.dto.response.OrderSummaryDTO;
import com.lakeserl.order_service.enums.OrderStatus;
import com.lakeserl.order_service.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/admin/orders")
@RequiredArgsConstructor
public class AdminOrderController {

    private final OrderService orderService;

    @GetMapping
    public ApiResponse<Page<OrderSummaryDTO>> getAllOrders(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String orderNumber,
            @RequestParam(required = false) LocalDateTime fromDate,
            @RequestParam(required = false) LocalDateTime toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {

        OrderStatus orderStatus = null;
        if (status != null && !status.isBlank()) {
            orderStatus = OrderStatus.valueOf(status.toUpperCase());
        }

        String[] sortParams = sort.split(",");
        Sort sortObj = Sort.by(Sort.Direction.fromString(sortParams[1]), sortParams[0]);
        Pageable pageable = PageRequest.of(page, size, sortObj);

        Page<OrderSummaryDTO> response = orderService.getAllOrdersAdmin(orderStatus, userId, orderNumber, fromDate, toDate, pageable);

        ApiResponse.PageInfo pageInfo = ApiResponse.PageInfo.builder()
                .currentPage(response.getNumber())
                .pageSize(response.getSize())
                .totalElements(response.getTotalElements())
                .totalPages(response.getTotalPages())
                .hasNext(response.hasNext())
                .hasPrevious(response.hasPrevious())
                .build();

        return ApiResponse.ok("All orders fetched successfully", response, pageInfo);
    }

    @GetMapping("/{orderNumber}")
    public ApiResponse<OrderDTO> getOrderDetail(@PathVariable String orderNumber) {
        OrderDTO response = orderService.getOrderDetail(orderNumber);
        return ApiResponse.ok(response);
    }

    @PatchMapping("/{orderNumber}/status")
    public ApiResponse<Void> updateOrderStatus(
            @PathVariable String orderNumber,
            @Valid @RequestBody UpdateOrderStatusRequest request) {
        String adminUsername = getAdminUsername();
        orderService.updateOrderStatusAdmin(orderNumber, request.status(), request.note(), adminUsername);
        return ApiResponse.ok("Order status updated successfully", null);
    }

    @PostMapping("/{orderNumber}/cancel")
    public ApiResponse<Void> cancelOrder(
            @PathVariable String orderNumber,
            @Valid @RequestBody CancelOrderRequest request) {
        orderService.cancelOrderSystem(orderNumber, "Cancelled by Admin: " + request.reason());
        return ApiResponse.ok("Order cancelled successfully by Admin", null);
    }

    @PostMapping("/{orderNumber}/confirm-return")
    public ApiResponse<Void> confirmReturn(@PathVariable String orderNumber) {
        String adminUsername = getAdminUsername();
        orderService.confirmReturnAdmin(orderNumber, adminUsername);
        return ApiResponse.ok("Return request confirmed successfully by Admin", null);
    }

    @GetMapping("/stats")
    public ApiResponse<OrderStatsDTO> getOrderStats() {
        OrderStatsDTO stats = orderService.getOrderStatsAdmin();
        return ApiResponse.ok("Order stats fetched successfully", stats);
    }

    @GetMapping("/export")
    public ResponseEntity<String> exportOrders(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String orderNumber,
            @RequestParam(required = false) LocalDateTime fromDate,
            @RequestParam(required = false) LocalDateTime toDate) {

        OrderStatus orderStatus = null;
        if (status != null && !status.isBlank()) {
            orderStatus = OrderStatus.valueOf(status.toUpperCase());
        }

        String csvData = orderService.exportOrdersCsv(orderStatus, userId, orderNumber, fromDate, toDate);
        String filename = "orders_" + System.currentTimeMillis() + ".csv";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csvData);
    }

    private String getAdminUsername() {
        return (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
