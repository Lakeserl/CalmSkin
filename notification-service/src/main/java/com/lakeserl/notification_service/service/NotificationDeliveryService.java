package com.lakeserl.notification_service.service;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lakeserl.notification_service.entity.Notification;
import com.lakeserl.notification_service.enums.NotificationChannel;
import com.lakeserl.notification_service.enums.NotificationStatus;
import com.lakeserl.notification_service.event.payload.RecipientContext;
import com.lakeserl.notification_service.repository.NotificationRepository;
import com.lakeserl.notification_service.service.channel.NotificationChannelHandler;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Delivers a single persisted notification row through its channel, off the
 * Kafka consumer thread. Records SENT/FAILED on the row; channel failures here
 * never propagate, they leave a FAILED row for the retry scheduler.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationDeliveryService {

    private static final int MAX_REASON_LENGTH = 1000;

    private final NotificationRepository notificationRepository;
    private final UserContactService userContactService;
    private final UnreadCountService unreadCountService;
    private final List<NotificationChannelHandler> handlerBeans;

    private Map<NotificationChannel, NotificationChannelHandler> handlers;

    @PostConstruct
    void init() {
        handlers = new EnumMap<>(NotificationChannel.class);
        for (NotificationChannelHandler handler : handlerBeans) {
            handlers.put(handler.channel(), handler);
        }
    }

    @Async("notificationExecutor")
    @Transactional
    public void deliver(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId).orElse(null);
        if (notification == null) {
            log.warn("Notification {} not found for delivery", notificationId);
            return;
        }
        if (notification.getStatus() == NotificationStatus.SENT
                || notification.getStatus() == NotificationStatus.READ) {
            return;
        }

        NotificationChannelHandler handler = handlers.get(notification.getChannel());
        if (handler == null) {
            notification.setStatus(NotificationStatus.FAILED);
            notification.setFailedReason("No handler for channel " + notification.getChannel());
            notificationRepository.save(notification);
            return;
        }

        RecipientContext recipient = userContactService.resolveRecipient(notification.getUserId(), null);
        try {
            handler.deliver(notification, recipient);
            notification.setStatus(NotificationStatus.SENT);
            notification.setSentAt(LocalDateTime.now());
            notification.setFailedReason(null);
        } catch (Exception ex) {
            log.error("Delivery failed for notificationId={} channel={}",
                    notificationId, notification.getChannel(), ex);
            notification.setStatus(NotificationStatus.FAILED);
            notification.setFailedReason(truncate(ex.getMessage()));
        }
        notificationRepository.save(notification);

        if (notification.getChannel() == NotificationChannel.IN_APP
                && notification.getStatus() == NotificationStatus.SENT) {
            unreadCountService.increment(notification.getUserId());
        }
    }

    private String truncate(String reason) {
        if (reason == null) {
            return "Unknown error";
        }
        return reason.length() > MAX_REASON_LENGTH ? reason.substring(0, MAX_REASON_LENGTH) : reason;
    }
}
