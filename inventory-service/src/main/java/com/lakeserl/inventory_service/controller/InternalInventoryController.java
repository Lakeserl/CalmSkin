package com.lakeserl.inventory_service.controller;

import com.lakeserl.inventory_service.dto.request.CheckStockBatchRequest;
import com.lakeserl.inventory_service.dto.request.ConfirmStockRequest;
import com.lakeserl.inventory_service.dto.request.ReleaseStockRequest;
import com.lakeserl.inventory_service.dto.request.ReserveStockRequest;
import com.lakeserl.inventory_service.dto.request.ReturnStockRequest;
import com.lakeserl.inventory_service.dto.response.ApiResponse;
import com.lakeserl.inventory_service.dto.response.ReservationResponse;
import com.lakeserl.inventory_service.dto.response.StockCheckBatchResponse;
import com.lakeserl.inventory_service.dto.response.StockCheckResponse;
import com.lakeserl.inventory_service.service.InventoryService;
import com.lakeserl.inventory_service.service.StockReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/inventory")
@RequiredArgsConstructor
@SecurityRequirement(name = "Internal Secret")
@Tag(name = "Internal Inventory APIs", description = "Internal endpoints for stock checks and reservations")
public class InternalInventoryController {
    private final InventoryService inventoryService;
    private final StockReservationService stockReservationService;

    @GetMapping("/check")
    @Operation(summary = "Check single inventory availability")
    public ApiResponse<StockCheckResponse> checkInventory(
            @RequestParam Long productId,
            @RequestParam(required = false) Long variantId,
            @RequestParam Integer quantity) {
        return ApiResponse.ok(inventoryService.checkStock(productId, variantId, quantity));
    }

    @PostMapping("/check-batch")
    @Operation(summary = "Check batch inventory availability")
    public ApiResponse<StockCheckBatchResponse> checkBatchInventory(
            @Valid @RequestBody CheckStockBatchRequest request) {
        return ApiResponse.ok(inventoryService.checkStockBatch(request.items()));
    }

    @PostMapping("/reserve")
    @Operation(summary = "Reserve inventory for order")
    public ResponseEntity<ApiResponse<ReservationResponse>> reserveInventory(
            @Valid @RequestBody ReserveStockRequest request) {
        ReservationResponse response = stockReservationService.reserveStock(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
    }

    @PostMapping("/release")
    @Operation(summary = "Release reserved inventory")
    public ApiResponse<ReservationResponse> releaseInventory(
            @Valid @RequestBody ReleaseStockRequest request) {
        return ApiResponse.ok(stockReservationService.releaseStock(request.orderId()));
    }

    @PostMapping("/confirm")
    @Operation(summary = "Confirm reserved inventory")
    public ApiResponse<ReservationResponse> confirmInventory(
            @Valid @RequestBody ConfirmStockRequest request) {
        return ApiResponse.ok(stockReservationService.confirmStock(request.orderId()));
    }

    @PostMapping("/return")
    @Operation(summary = "Return items from order")
    public ApiResponse<ReservationResponse> returnInventory(
            @Valid @RequestBody ReturnStockRequest request) {
        return ApiResponse.ok(stockReservationService.returnStock(request));
    }
}
