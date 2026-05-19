package com.lakeserl.inventory_service.entity;

import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "inventories", uniqueConstraints = {
    @UniqueConstraint(name = "uq_inventory_product_variant", columnNames = {"product_id", "variant_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Inventory {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "variant_id", nullable = true)
    private Long variantId;

    @Column(name = "quantity_available", nullable = false)
    private Integer quantityAvailable;

    @Column(name = "quantity_reserved", nullable=false)
    private Integer quantityReserved;

    @Column(name = "quantity_sold", nullable=false)
    private Integer quantitySold;

    @Column(name = "low_stock_threshold", nullable=false, columnDefinition = "INTEGER DEFAULT 10")
    private Integer lowStockThreshold;

    @Column(name = "warehouse_location", length = 50)
    private String warehouseLocation;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (lowStockThreshold == null) {
            lowStockThreshold = 10;
        }
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Integer getTotalQuantity() {
        return quantityAvailable + quantityReserved;
    }

}
