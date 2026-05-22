package com.lakeserl.promotion_service.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.lakeserl.promotion_service.entity.PromotionUsageLock;

@Repository
public interface PromotionUsageLockRepository extends JpaRepository<PromotionUsageLock, Long> {

    List<PromotionUsageLock> findByOrderId(String orderId);

    List<PromotionUsageLock> findByExpiresAtBefore(LocalDateTime cutoff);

    long countByPromotionId(Long promotionId);

    boolean existsByPromotionIdAndUserId(Long promotionId, UUID userId);
}
