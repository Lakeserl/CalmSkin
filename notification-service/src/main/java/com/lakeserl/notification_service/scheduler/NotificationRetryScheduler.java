package com.lakeserl.notification_service.scheduler;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.lakeserl.notification_service.config.properties.NotificationProperties;
import com.lakeserl.notification_service.entity.Notification;
import com.lakeserl.notification_service.enums.NotificationStatus;
import com.lakeserl.notification_service.repository.NotificationRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Re-queues FAILED notifications for another delivery attempt, up to the
 * configured retry ceiling. The sender scheduler then picks the PENDING rows
 * back up.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationRetryScheduler {

    private static final int BATCH_SIZE = 200;

    private final NotificationRepository notificationRepository;
    private final NotificationProperties props;

    @Scheduled(fixedDelay = 300_000)
    @Transactional
    public void retryFailed() {
        List<Notification> failed = notificationRepository.findByStatusAndRetryCountLessThan(
                NotificationStatus.FAILED, props.maxRetry(), PageRequest.of(0, BATCH_SIZE));
        if (failed.isEmpty()) {
            return;
        }
        for (Notification notification : failed) {
            notification.setRetryCount(notification.getRetryCount() + 1);
            notification.setStatus(NotificationStatus.PENDING);
            notificationRepository.save(notification);
        }
        log.info("Re-queued {} failed notifications for retry", failed.size());
    }
}
