package com.lakeserl.user_service.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.lakeserl.user_service.model.entity.RecentlyViewed;
import com.lakeserl.user_service.model.entity.RecentlyViewedId;

public interface RecentlyViewedRepository extends JpaRepository<RecentlyViewed, RecentlyViewedId> {

    @Modifying
    @Query(value = """
            INSERT INTO recently_viewed (user_id, product_id, viewed_at)
            VALUES (:userId, :productId, now())
            ON CONFLICT (user_id, product_id)
            DO UPDATE SET viewed_at = EXCLUDED.viewed_at
            """, nativeQuery = true)
    void upsert(@Param("userId") UUID userId, @Param("productId") Long productId);

    @Query("SELECT r.productId FROM RecentlyViewed r WHERE r.userId = :userId ORDER BY r.viewedAt DESC")
    List<Long> findProductIdsByUserId(@Param("userId") UUID userId, Pageable pageable);
}
