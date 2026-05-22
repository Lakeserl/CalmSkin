package com.lakeserl.promotion_service.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.lakeserl.promotion_service.entity.BundleItem;

@Repository
public interface BundleItemRepository extends JpaRepository<BundleItem, Long> {

    List<BundleItem> findByPromotionId(Long promotionId);
}
