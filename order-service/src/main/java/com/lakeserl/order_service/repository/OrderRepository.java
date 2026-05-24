package com.lakeserl.order_service.repository;

import com.lakeserl.order_service.entity.Order;
import com.lakeserl.order_service.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, Long>, JpaSpecificationExecutor<Order> {
    Optional<Order> findByOrderNumber(String orderNumber);
    
    Optional<Order> findByOrderNumberAndUserId(String orderNumber, UUID userId);

    Page<Order> findByUserId(UUID userId, Pageable pageable);

    Page<Order> findByUserIdAndStatus(UUID userId, OrderStatus status, Pageable pageable);
    
    List<Order> findByStatusAndCreatedAtBefore(OrderStatus status, LocalDateTime dateTime);
    
    boolean existsByOrderNumber(String orderNumber);

    @Query("SELECT o.status, COUNT(o) FROM Order o GROUP BY o.status")
    List<Object[]> countOrdersByStatus();

    @Query("SELECT o FROM Order o WHERE (:status IS NULL OR o.status = :status) " +
           "AND (:userId IS NULL OR o.userId = :userId) " +
           "AND (:orderNumber IS NULL OR LOWER(o.orderNumber) LIKE LOWER(CONCAT('%', :orderNumber, '%'))) " +
           "AND (CAST(:fromDate AS timestamp) IS NULL OR o.createdAt >= :fromDate) " +
           "AND (CAST(:toDate AS timestamp) IS NULL OR o.createdAt <= :toDate)")
    Page<Order> findAllWithFilters(@Param("status") OrderStatus status,
                                   @Param("userId") UUID userId,
                                   @Param("orderNumber") String orderNumber,
                                   @Param("fromDate") LocalDateTime fromDate,
                                   @Param("toDate") LocalDateTime toDate,
                                   Pageable pageable);

    /**
     * Returns distinct productIds purchased by a user in DELIVERED orders within the last N days.
     * Used by product-service Recommendations algorithm for exclusion + brand-affinity scoring.
     */
    @Query("SELECT DISTINCT i.productId FROM Order o JOIN o.items i " +
           "WHERE o.userId = :userId AND o.status = 'DELIVERED' " +
           "AND o.deliveredAt >= :since")
    List<Long> findDeliveredProductIdsByUserSince(@Param("userId") UUID userId,
                                                  @Param("since") LocalDateTime since);
}
