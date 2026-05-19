package com.lakeserl.inventory_service.repository;

import com.lakeserl.inventory_service.entity.Inventory;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import jakarta.persistence.LockModeType;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {
        @Query("""
                        select i from Inventory i
                        where i.productId = :productId
                            and ((:variantId is null and i.variantId is null) or i.variantId = :variantId)
                        """)
        Optional<Inventory> findByProductIdAndVariantId(Long productId, Long variantId);

        @Lock(LockModeType.PESSIMISTIC_WRITE)
        @Query("""
                        select i from Inventory i
                        where i.productId = :productId
                            and ((:variantId is null and i.variantId is null) or i.variantId = :variantId)
                        """)
        Optional<Inventory> findByProductIdAndVariantIdForUpdate(Long productId, Long variantId);

        @Lock(LockModeType.PESSIMISTIC_WRITE)
        @Query("select i from Inventory i where i.id = :id")
        Optional<Inventory> findByIdForUpdate(Long id);

        @Query("""
                        select i from Inventory i
                        where (:productId is null or i.productId = :productId)
                            and (:variantId is null or i.variantId = :variantId)
                        """)
        Page<Inventory> findByFilters(Long productId, Long variantId, Pageable pageable);

        @Query("select i from Inventory i where i.quantityAvailable < i.lowStockThreshold")
        Page<Inventory> findLowStock(Pageable pageable);

        @Query("select i from Inventory i where i.quantityAvailable = 0")
        Page<Inventory> findOutOfStock(Pageable pageable);

        @Query("select sum(i.quantityAvailable), sum(i.quantityReserved), sum(i.quantitySold) from Inventory i")
        Object[] fetchStats();
}
