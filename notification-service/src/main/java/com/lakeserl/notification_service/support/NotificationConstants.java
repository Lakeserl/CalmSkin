package com.lakeserl.notification_service.support;

import java.util.UUID;

/** Shared constants for the notification service. */
public final class NotificationConstants {

    private NotificationConstants() {
    }

    /**
     * Synthetic user id that owns the shared admin notification feed. Events
     * targeted at staff (low stock, new review) are stored as IN_APP rows under
     * this id and read back through the admin API.
     */
    public static final UUID ADMIN_FEED_USER_ID = new UUID(0L, 0L);
}
