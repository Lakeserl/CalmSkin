package com.lakeserl.inventory_service.service;

import com.lakeserl.inventory_service.dto.request.AdjustStockRequest;
import com.lakeserl.inventory_service.dto.request.CheckStockRequest;
import com.lakeserl.inventory_service.dto.response.InventoryDTO;
import com.lakeserl.inventory_service.dto.response.StockCheckBatchResponse;
import com.lakeserl.inventory_service.dto.response.StockCheckResponse;
import com.lakeserl.inventory_service.entity.Inventory;
import com.lakeserl.inventory_service.entity.StockMovement;
import com.lakeserl.inventory_service.enums.MovementType;
import com.lakeserl.inventory_service.enums.ReferenceType;
import com.lakeserl.inventory_service.event.producer.InventoryEventProducer;
import com.lakeserl.inventory_service.exception.InventoryNotFoundException;
import com.lakeserl.inventory_service.repository.InventoryRepository;
import com.lakeserl.inventory_service.repository.StockMovementRepository;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {
    private static final int DEFAULT_LOW_STOCK_THRESHOLD = 10;

    private final InventoryRepository inventoryRepository;
    private final StockMovementRepository stockMovementRepository;
    private final InventoryEventProducer inventoryEventProducer;

    @Override
    public StockCheckResponse checkStock(Long productId, Long variantId, Integer quantity) {
        Inventory inventory = inventoryRepository.findByProductIdAndVariantId(productId, variantId).orElse(null);
        Integer availableQuantity = inventory == null ? null : inventory.getQuantityAvailable();
        int availableQuantityValue = availableQuantity == null ? 0 : availableQuantity;
        boolean available = inventory != null && availableQuantityValue >= quantity;

        return StockCheckResponse.builder()
                .productId(productId)
                .variantId(variantId)
                .available(available)
                .quantityAvailable(availableQuantityValue)
                .build();
    }

    @Override
    public StockCheckBatchResponse checkStockBatch(List<CheckStockRequest> items) {
        List<StockCheckResponse> responses = items.stream()
                .map(item -> checkStock(item.productId(), item.variantId(), item.quantity()))
                .toList();
        return StockCheckBatchResponse.builder()
                .items(responses)
                .build();
    }

    @Override
    public Page<InventoryDTO> getInventories(Long productId, Long variantId, Pageable pageable) {
        return inventoryRepository.findByFilters(productId, variantId, pageable)
                .map(inventory -> toInventoryDTO(inventory, null));
    }

    @Override
    public InventoryDTO getInventoryDetail(Long inventoryId) {
        Inventory inventory = inventoryRepository.findById(inventoryId)
                .orElseThrow(() -> new InventoryNotFoundException("Inventory not found"));
        List<StockMovement> movements = stockMovementRepository
                .findTop20ByInventoryIdOrderByCreatedAtDesc(inventoryId);
        return toInventoryDTO(inventory, movements);
    }

    @Override
    @Transactional
    public InventoryDTO updateInventorySettings(Long inventoryId, Integer lowStockThreshold, String warehouseLocation) {
        Inventory inventory = inventoryRepository.findByIdForUpdate(inventoryId)
                .orElseThrow(() -> new InventoryNotFoundException("Inventory not found"));

        if (lowStockThreshold != null) {
            inventory.setLowStockThreshold(lowStockThreshold);
        }
        if (warehouseLocation != null) {
            inventory.setWarehouseLocation(warehouseLocation);
        }

        Inventory saved = inventoryRepository.save(inventory);
        return toInventoryDTO(saved, null);
    }

    @Override
    @Transactional
    public InventoryDTO importStock(AdjustStockRequest request, String createdBy) {
        Inventory inventory = inventoryRepository
                .findByProductIdAndVariantIdForUpdate(request.productId(), request.variantId())
                .orElse(null);

        Integer existingAvailable = inventory == null ? null : inventory.getQuantityAvailable();
        int previousAvailable = existingAvailable == null ? 0 : existingAvailable;

        if (inventory == null) {
            inventory = Inventory.builder()
                    .productId(request.productId())
                    .variantId(request.variantId())
                    .quantityAvailable(request.quantity())
                    .quantityReserved(0)
                    .quantitySold(0)
                    .lowStockThreshold(DEFAULT_LOW_STOCK_THRESHOLD)
                    .warehouseLocation(null)
                    .build();
        } else {
            inventory.setQuantityAvailable(inventory.getQuantityAvailable() + request.quantity());
        }

        Inventory saved = inventoryRepository.save(inventory);

        StockMovement movement = StockMovement.builder()
                .inventoryId(saved.getId())
                .movementType(MovementType.IN)
                .quantity(request.quantity())
                .referenceType(ReferenceType.MANUAL_IMPORT)
                .note(request.note())
                .createdBy(createdBy)
                .build();
        stockMovementRepository.save(movement);

        inventoryEventProducer.publishStockAlerts(saved, previousAvailable);

        return toInventoryDTO(saved, null);
    }

    @Override
    public Page<InventoryDTO.StockMovementDTO> getMovements(Long inventoryId, Pageable pageable) {
        return stockMovementRepository.findByInventoryIdOrderByCreatedAtDesc(inventoryId, pageable)
                .map(this::toMovementDTO);
    }

    @Override
    public Page<InventoryDTO> getLowStock(Pageable pageable) {
        return inventoryRepository.findLowStock(pageable)
                .map(inventory -> toInventoryDTO(inventory, null));
    }

    @Override
    public Page<InventoryDTO> getOutOfStock(Pageable pageable) {
        return inventoryRepository.findOutOfStock(pageable)
                .map(inventory -> toInventoryDTO(inventory, null));
    }

    @Override
    public Map<String, Long> getStats() {
        Object[] stats = inventoryRepository.fetchStats();
        long totalAvailable = stats == null || stats[0] == null ? 0L : ((Number) stats[0]).longValue();
        long totalReserved = stats == null || stats[1] == null ? 0L : ((Number) stats[1]).longValue();
        long totalSold = stats == null || stats[2] == null ? 0L : ((Number) stats[2]).longValue();
        return Map.of(
                "totalAvailable", totalAvailable,
                "totalReserved", totalReserved,
                "totalSold", totalSold
        );
    }

    private InventoryDTO toInventoryDTO(Inventory inventory, List<StockMovement> movements) {
        List<InventoryDTO.StockMovementDTO> movementDTOs = movements == null ? null : movements.stream()
                .map(this::toMovementDTO)
                .toList();

        return InventoryDTO.builder()
                .id(inventory.getId())
                .productId(inventory.getProductId())
                .variantId(inventory.getVariantId())
                .quantityAvailable(inventory.getQuantityAvailable())
                .quantityReserved(inventory.getQuantityReserved())
                .quantitySold(inventory.getQuantitySold())
                .lowStockThreshold(inventory.getLowStockThreshold())
                .warehouseLocation(inventory.getWarehouseLocation())
                .createdAt(toInstant(inventory.getCreatedAt()))
                .updatedAt(toInstant(inventory.getUpdatedAt()))
                .movements(movementDTOs)
                .build();
    }

    private InventoryDTO.StockMovementDTO toMovementDTO(StockMovement movement) {
        return InventoryDTO.StockMovementDTO.builder()
                .id(movement.getId())
                .movementType(movement.getMovementType())
                .quantity(movement.getQuantity())
                .referenceId(movement.getReferenceId())
                .referenceType(movement.getReferenceType())
                .note(movement.getNote())
                .createdBy(movement.getCreatedBy())
                .createdAt(toInstant(movement.getCreatedAt()))
                .build();
    }

    private Instant toInstant(LocalDateTime value) {
        return value == null ? null : value.atOffset(ZoneOffset.UTC).toInstant();
    }
}
