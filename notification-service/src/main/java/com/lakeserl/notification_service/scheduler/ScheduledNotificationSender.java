package com.lakeserl.notification_service.scheduler;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.lakeserl.notification_service.entity.Notification;
import com.lakeserl.notification_service.enums.NotificationStatus;
import com.lakeserl.notification_service.repository.NotificationRepository;
import com.lakeserl.notification_service.service.NotificationDeliveryService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Sends notifications whose scheduled time has arrived, and recovers PENDING
 * rows that were never delivered (e.g. a crash between persist and delivery).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduledNotificationSender {

    private static final int BATCH_SIZE = 200;
    private static final int STUCK_GRACE_MINUTES = 5;

    private final NotificationRepository notificationRepository;
    private final NotificationDeliveryService deliveryService;

    @Scheduled(fixedDelay = 30_000)
    public void sendDue() {
        LocalDateTime now = LocalDateTime.now();
        List<Notification> due = notificationRepository.findDue(
                NotificationStatus.SCHEDULED,
                NotificationStatus.PENDING,
                now,
                now.minusMinutes(STUCK_GRACE_MINUTES),
                PageRequest.of(0, BATCH_SIZE));
        if (due.isEmpty()) {
            return;
        }
        log.info("Delivering {} due notifications", due.size());
        for (Notification notification : due) {
            deliveryService.deliver(notification.getId());
        }
    }
}
