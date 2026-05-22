package com.lakeserl.promotion_service.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.lakeserl.promotion_service.entity.Promotion;
import com.lakeserl.promotion_service.enums.PromotionStatus;
import com.lakeserl.promotion_service.enums.PromotionType;

@Repository
public interface PromotionRepository extends JpaRepository<Promotion, Long> {

    Optional<Promotion> findByCode(String code);

    List<Promotion> findByStatus(PromotionStatus status);

    List<Promotion> findByStatusAndStartsAtLessThanEqual(PromotionStatus status, LocalDateTime cutoff);

    List<Promotion> findByStatusAndEndsAtLessThan(PromotionStatus status, LocalDateTime cutoff);

    @Query("SELECT p FROM Promotion p "
            + "WHERE (:type IS NULL OR p.type = :type) "
            + "AND (:status IS NULL OR p.status = :status)")
    Page<Promotion> search(@Param("type") PromotionType type,
                           @Param("status") PromotionStatus status,
                           Pageable pageable);
}
