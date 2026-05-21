package com.lakeserl.notification_service.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.lakeserl.notification_service.entity.WebPushSubscription;

@Repository
public interface WebPushSubscriptionRepository extends JpaRepository<WebPushSubscription, Long> {

    List<WebPushSubscription> findByUserIdAndIsActiveTrue(UUID userId);

    Optional<WebPushSubscription> findByEndpoint(String endpoint);

    void deleteByEndpointAndUserId(String endpoint, UUID userId);

    /** Removes subscriptions the browser relays have rejected or that went stale. */
    @Modifying
    @Query("DELETE FROM WebPushSubscription s WHERE s.isActive = false OR s.lastUsedAt < :cutoff")
    int deleteStale(@Param("cutoff") LocalDateTime cutoff);
}
