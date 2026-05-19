package com.lakeserl.inventory_service.service;

import com.lakeserl.inventory_service.dto.request.AdjustStockRequest;
import com.lakeserl.inventory_service.dto.request.CheckStockRequest;
import com.lakeserl.inventory_service.dto.response.InventoryDTO;
import com.lakeserl.inventory_service.dto.response.StockCheckBatchResponse;
import com.lakeserl.inventory_service.dto.response.StockCheckResponse;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface InventoryService {
    StockCheckResponse checkStock(Long productId, Long variantId, Integer quantity);

    StockCheckBatchResponse checkStockBatch(List<CheckStockRequest> items);

    Page<InventoryDTO> getInventories(Long productId, Long variantId, Pageable pageable);

    InventoryDTO getInventoryDetail(Long inventoryId);

    InventoryDTO updateInventorySettings(Long inventoryId, Integer lowStockThreshold, String warehouseLocation);

    InventoryDTO importStock(AdjustStockRequest request, String createdBy);

    Page<InventoryDTO.StockMovementDTO> getMovements(Long inventoryId, Pageable pageable);

    Page<InventoryDTO> getLowStock(Pageable pageable);

    Page<InventoryDTO> getOutOfStock(Pageable pageable);

    Map<String, Long> getStats();
}
