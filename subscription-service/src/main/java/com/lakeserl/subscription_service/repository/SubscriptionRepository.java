package com.lakeserl.subscription_service.repository;

import com.lakeserl.subscription_service.entity.Subscription;
import com.lakeserl.subscription_service.enums.SubscriptionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {

    Page<Subscription> findByUserId(UUID userId, Pageable pageable);

    Page<Subscription> findByUserIdAndStatus(UUID userId, SubscriptionStatus status, Pageable pageable);

    Page<Subscription> findByStatus(SubscriptionStatus status, Pageable pageable);

    Optional<Subscription> findByIdAndUserId(UUID id, UUID userId);

    /**
     * Fetches ACTIVE subscriptions whose next order is due (used by the daily scheduler).
     * Ordered by nextOrderDueAt ASC so oldest-due subscriptions are processed first.
     */
    @Query("""
            SELECT s FROM Subscription s
            WHERE s.status = 'ACTIVE'
              AND s.nextOrderDueAt <= :now
            ORDER BY s.nextOrderDueAt ASC
            """)
    List<Subscription> findDueSubscriptions(@Param("now") LocalDateTime now);
}
