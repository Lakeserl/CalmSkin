package com.lakeserl.promotion_service.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.lakeserl.promotion_service.entity.PromotionUsage;
import com.lakeserl.promotion_service.enums.UsageStatus;

@Repository
public interface PromotionUsageRepository extends JpaRepository<PromotionUsage, Long> {

    long countByPromotionIdAndStatus(Long promotionId, UsageStatus status);

    long countByPromotionIdAndUserIdAndStatus(Long promotionId, UUID userId, UsageStatus status);

    List<PromotionUsage> findByOrderId(String orderId);

    boolean existsByOrderIdAndPromotionId(String orderId, Long promotionId);

    Page<PromotionUsage> findByPromotionId(Long promotionId, Pageable pageable);

    @Query("SELECT COALESCE(SUM(u.discountAmount), 0) FROM PromotionUsage u "
            + "WHERE u.promotionId = :promotionId AND u.status = :status")
    BigDecimal sumDiscountByPromotionIdAndStatus(@Param("promotionId") Long promotionId,
                                                 @Param("status") UsageStatus status);
}
