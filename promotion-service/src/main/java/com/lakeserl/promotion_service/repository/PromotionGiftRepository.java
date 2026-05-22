package com.lakeserl.promotion_service.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.lakeserl.promotion_service.entity.PromotionGift;

@Repository
public interface PromotionGiftRepository extends JpaRepository<PromotionGift, Long> {

    List<PromotionGift> findByPromotionId(Long promotionId);

    List<PromotionGift> findByPromotionIdIn(Collection<Long> promotionIds);
}
