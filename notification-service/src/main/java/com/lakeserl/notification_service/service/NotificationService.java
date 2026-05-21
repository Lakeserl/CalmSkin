package com.lakeserl.notification_service.service;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lakeserl.notification_service.dto.response.NotificationResponse;
import com.lakeserl.notification_service.entity.Notification;
import com.lakeserl.notification_service.enums.NotificationChannel;
import com.lakeserl.notification_service.enums.NotificationStatus;
import com.lakeserl.notification_service.exception.BadRequestException;
import com.lakeserl.notification_service.exception.ResourceNotFoundException;
import com.lakeserl.notification_service.repository.NotificationRepository;

import lombok.RequiredArgsConstructor;

/**
 * Read/update API for a user's in-app notification feed. The feed only exposes
 * IN_APP rows; EMAIL and WEB_PUSH rows are delivery audit records.
 */
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UnreadCountService unreadCountService;

    public Page<NotificationResponse> list(UUID userId, String status, Pageable pageable) {
        Page<Notification> page;
        if ("UNREAD".equalsIgnoreCase(status)) {
            page = notificationRepository.findByUserIdAndChannelAndReadAtIsNullOrderByCreatedAtDesc(
                    userId, NotificationChannel.IN_APP, pageable);
        } else if ("READ".equalsIgnoreCase(status)) {
            page = notificationRepository.findByUserIdAndChannelAndReadAtIsNotNullOrderByCreatedAtDesc(
                    userId, NotificationChannel.IN_APP, pageable);
        } else {
            page = notificationRepository.findByUserIdAndChannelOrderByCreatedAtDesc(
                    userId, NotificationChannel.IN_APP, pageable);
        }
        return page.map(this::toResponse);
    }

    public long unreadCount(UUID userId) {
        return unreadCountService.get(userId);
    }

    @Transactional
    public void markRead(UUID userId, Long id) {
        Notification notification = notificationRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
        if (notification.getChannel() != NotificationChannel.IN_APP) {
            throw new BadRequestException("Only in-app notifications can be marked read");
        }
        if (notification.getReadAt() == null) {
            notification.setReadAt(LocalDateTime.now());
            notification.setStatus(NotificationStatus.READ);
            notificationRepository.save(notification);
            unreadCountService.decrement(userId);
        }
    }

    @Transactional
    public int markAllRead(UUID userId) {
        int updated = notificationRepository.markAllRead(
                userId, NotificationChannel.IN_APP, NotificationStatus.READ, LocalDateTime.now());
        unreadCountService.reset(userId);
        return updated;
    }

    @Transactional
    public void delete(UUID userId, Long id) {
        Notification notification = notificationRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
        boolean wasUnread = notification.getChannel() == NotificationChannel.IN_APP
                && notification.getReadAt() == null;
        notificationRepository.delete(notification);
        if (wasUnread) {
            unreadCountService.decrement(userId);
        }
    }

    private NotificationResponse toResponse(Notification n) {
        return new NotificationResponse(
                n.getId(),
                n.getChannel().name(),
                n.getTemplateCode(),
                n.getSubject(),
                n.getBody(),
                n.getReferenceId(),
                n.getReferenceType(),
                n.getStatus().name(),
                n.getPriority().name(),
                n.getReadAt() != null,
                n.getReadAt(),
                n.getMetadata(),
                n.getCreatedAt());
    }
}
