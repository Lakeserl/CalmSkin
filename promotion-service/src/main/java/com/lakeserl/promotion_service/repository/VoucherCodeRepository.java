package com.lakeserl.promotion_service.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.lakeserl.promotion_service.entity.VoucherCode;

@Repository
public interface VoucherCodeRepository extends JpaRepository<VoucherCode, Long> {

    Optional<VoucherCode> findByCode(String code);

    boolean existsByCode(String code);

    List<VoucherCode> findByPromotionId(Long promotionId);
}
