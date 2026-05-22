package com.lakeserl.promotion_service.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.lakeserl.promotion_service.entity.VoucherAssignment;

@Repository
public interface VoucherAssignmentRepository extends JpaRepository<VoucherAssignment, Long> {

    List<VoucherAssignment> findByUserId(UUID userId);

    boolean existsByPromotionIdAndUserId(Long promotionId, UUID userId);
}
