package com.lakeserl.notification_service.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.lakeserl.notification_service.entity.Notification;
import com.lakeserl.notification_service.enums.NotificationChannel;
import com.lakeserl.notification_service.enums.NotificationStatus;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByUserIdAndChannelOrderByCreatedAtDesc(
            UUID userId, NotificationChannel channel, Pageable pageable);

    Page<Notification> findByUserIdAndChannelAndReadAtIsNullOrderByCreatedAtDesc(
            UUID userId, NotificationChannel channel, Pageable pageable);

    Page<Notification> findByUserIdAndChannelAndReadAtIsNotNullOrderByCreatedAtDesc(
            UUID userId, NotificationChannel channel, Pageable pageable);

    long countByUserIdAndChannelAndReadAtIsNull(UUID userId, NotificationChannel channel);

    Optional<Notification> findByIdAndUserId(Long id, UUID userId);

    /**
     * Notifications due for sending: SCHEDULED rows whose time has come, plus
     * PENDING rows that look stuck (older than the grace cutoff) so a crash
     * mid-delivery is still recovered.
     */
    @Query("SELECT n FROM Notification n WHERE "
            + "(n.status = :scheduled AND n.scheduledAt <= :now) "
            + "OR (n.status = :pending AND n.createdAt <= :stuckCutoff)")
    List<Notification> findDue(@Param("scheduled") NotificationStatus scheduled,
                               @Param("pending") NotificationStatus pending,
                               @Param("now") LocalDateTime now,
                               @Param("stuckCutoff") LocalDateTime stuckCutoff,
                               Pageable pageable);

    List<Notification> findByStatusAndRetryCountLessThan(
            NotificationStatus status, int maxRetry, Pageable pageable);

    long countByStatusAndCreatedAtGreaterThanEqual(NotificationStatus status, LocalDateTime since);

    long countByChannelAndCreatedAtGreaterThanEqual(NotificationChannel channel, LocalDateTime since);

    @Modifying
    @Query("UPDATE Notification n SET n.status = :read, n.readAt = :ts "
            + "WHERE n.userId = :userId AND n.channel = :channel AND n.readAt IS NULL")
    int markAllRead(@Param("userId") UUID userId,
                    @Param("channel") NotificationChannel channel,
                    @Param("read") NotificationStatus read,
                    @Param("ts") LocalDateTime ts);
}
