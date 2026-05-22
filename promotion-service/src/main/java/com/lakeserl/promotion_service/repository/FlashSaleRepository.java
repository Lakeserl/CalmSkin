package com.lakeserl.promotion_service.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.lakeserl.promotion_service.entity.FlashSale;

import jakarta.persistence.LockModeType;

@Repository
public interface FlashSaleRepository extends JpaRepository<FlashSale, Long> {

    List<FlashSale> findByPromotionId(Long promotionId);

    List<FlashSale> findByPromotionIdIn(Collection<Long> promotionIds);

    /** Finds the flash-sale line for a product/variant ({@code variantId} may be null). */
    @Query("SELECT f FROM FlashSale f WHERE f.productId = :productId "
            + "AND ((:variantId IS NULL AND f.variantId IS NULL) OR f.variantId = :variantId)")
    Optional<FlashSale> findForProduct(@Param("productId") Long productId,
                                       @Param("variantId") Long variantId);

    /** Loads a flash-sale row under a pessimistic write lock for slot mutation. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT f FROM FlashSale f WHERE f.id = :id")
    Optional<FlashSale> lockById(@Param("id") Long id);
}
