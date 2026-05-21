package com.lakeserl.notification_service.service.channel;

import com.lakeserl.notification_service.entity.Notification;
import com.lakeserl.notification_service.enums.NotificationChannel;
import com.lakeserl.notification_service.event.payload.RecipientContext;

/**
 * Strategy for delivering a notification through one channel. An implementation
 * must throw on a retryable failure and return normally on success; the caller
 * ({@code NotificationDeliveryService}) records SENT/FAILED accordingly.
 */
public interface NotificationChannelHandler {

    NotificationChannel channel();

    void deliver(Notification notification, RecipientContext recipient) throws Exception;
}
