package com.lakeserl.inventory_service.controller;

import com.lakeserl.inventory_service.dto.request.AdjustStockRequest;
import com.lakeserl.inventory_service.dto.response.ApiResponse;
import com.lakeserl.inventory_service.dto.response.InventoryDTO;
import com.lakeserl.inventory_service.service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/inventory")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Auth")
@Tag(name = "Admin Inventory APIs", description = "Admin endpoints for inventory management")
public class AdminInventoryController {

    private final InventoryService inventoryService;

    @GetMapping
    @Operation(summary = "List inventories with filters")
    public ApiResponse<Page<InventoryDTO>> listInventories(
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) Long variantId,
            Pageable pageable) {
        Page<InventoryDTO> page = inventoryService.getInventories(productId, variantId, pageable);
        return ApiResponse.ok("OK", page, toPageInfo(page));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get inventory detail with recent movements")
    public ApiResponse<InventoryDTO> getInventory(@PathVariable Long id) {
        return ApiResponse.ok(inventoryService.getInventoryDetail(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update inventory threshold and location")
    public ApiResponse<InventoryDTO> updateInventory(
            @PathVariable Long id,
            @RequestParam(required = false) Integer lowStockThreshold,
            @RequestParam(required = false) String warehouseLocation) {
        return ApiResponse.ok(inventoryService.updateInventorySettings(id, lowStockThreshold, warehouseLocation));
    }

    @PostMapping("/import")
    @Operation(summary = "Manual stock import")
    public ApiResponse<InventoryDTO> importStock(
            @Valid @RequestBody AdjustStockRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        String createdBy = userId == null || userId.isBlank() ? "SYSTEM" : userId;
        return ApiResponse.ok("Stock imported", inventoryService.importStock(request, createdBy));
    }

    @GetMapping("/{id}/movements")
    @Operation(summary = "Get full movement history")
    public ApiResponse<Page<InventoryDTO.StockMovementDTO>> getMovements(@PathVariable Long id, Pageable pageable) {
        Page<InventoryDTO.StockMovementDTO> page = inventoryService.getMovements(id, pageable);
        return ApiResponse.ok("OK", page, toPageInfo(page));
    }

    @GetMapping("/low-stock")
    @Operation(summary = "Get low stock inventories")
    public ApiResponse<Page<InventoryDTO>> getLowStock(Pageable pageable) {
        Page<InventoryDTO> page = inventoryService.getLowStock(pageable);
        return ApiResponse.ok("OK", page, toPageInfo(page));
    }

    @GetMapping("/out-of-stock")
    @Operation(summary = "Get out of stock inventories")
    public ApiResponse<Page<InventoryDTO>> getOutOfStock(Pageable pageable) {
        Page<InventoryDTO> page = inventoryService.getOutOfStock(pageable);
        return ApiResponse.ok("OK", page, toPageInfo(page));
    }

    @GetMapping("/stats")
    @Operation(summary = "Get inventory statistics")
    public ApiResponse<Map<String, Long>> getStats() {
        return ApiResponse.ok(inventoryService.getStats());
    }

    private ApiResponse.PageInfo toPageInfo(Page<?> page) {
        return ApiResponse.PageInfo.builder()
                .currentPage(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .hasNext(page.hasNext())
                .hasPrevious(page.hasPrevious())
                .build();
    }
}
