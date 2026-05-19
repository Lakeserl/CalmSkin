package com.lakeserl.inventory_service.dto.response;

import com.lakeserl.inventory_service.enums.MovementType;
import com.lakeserl.inventory_service.enums.ReferenceType;
import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryDTO {
    private Long id;
    private Long productId;
    private Long variantId;
    private Integer quantityAvailable;
    private Integer quantityReserved;
    private Integer quantitySold;
    private Integer lowStockThreshold;
    private String warehouseLocation;
    private Instant createdAt;
    private Instant updatedAt;
    private List<StockMovementDTO> movements;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StockMovementDTO {
        private Long id;
        private MovementType movementType;
        private Integer quantity;
        private String referenceId;
        private ReferenceType referenceType;
        private String note;
        private String createdBy;
        private Instant createdAt;
    }
}
