package com.lakeserl.inventory_service.repository;

import com.lakeserl.inventory_service.entity.StockMovement;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {
	Page<StockMovement> findByInventoryIdOrderByCreatedAtDesc(Long inventoryId, Pageable pageable);

	List<StockMovement> findTop20ByInventoryIdOrderByCreatedAtDesc(Long inventoryId);
}
