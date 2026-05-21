package com.lakeserl.notification_service.service.channel;

import org.springframework.stereotype.Component;

import com.lakeserl.notification_service.entity.Notification;
import com.lakeserl.notification_service.enums.NotificationChannel;
import com.lakeserl.notification_service.event.payload.RecipientContext;

import lombok.extern.slf4j.Slf4j;

/**
 * In-app delivery. The persisted notification row is itself the delivery, so
 * there is nothing external to do here; the caller flips the row to SENT and
 * bumps the unread counter.
 */
@Slf4j
@Component
public class InAppNotificationChannel implements NotificationChannelHandler {

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.IN_APP;
    }

    @Override
    public void deliver(Notification notification, RecipientContext recipient) {
        log.debug("In-app notification id={} ready for user={}", notification.getId(), notification.getUserId());
    }
}
