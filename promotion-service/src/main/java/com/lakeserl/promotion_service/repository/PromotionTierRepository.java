package com.lakeserl.promotion_service.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.lakeserl.promotion_service.entity.PromotionTier;

@Repository
public interface PromotionTierRepository extends JpaRepository<PromotionTier, Long> {

    List<PromotionTier> findByPromotionIdOrderBySortOrderAsc(Long promotionId);
}
