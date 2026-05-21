package com.lakeserl.notification_service.event.payload;

import java.util.UUID;

/**
 * Resolved delivery details for one recipient. {@code email} may be null when
 * the user has never been seen by a user.* event; the email channel then skips.
 */
public record RecipientContext(
        UUID userId,
        String email,
        String fullName,
        String locale
) {
    public boolean hasEmail() {
        return email != null && !email.isBlank();
    }
}
