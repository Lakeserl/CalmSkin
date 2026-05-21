package com.lakeserl.notification_service.service;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lakeserl.notification_service.config.properties.NotificationProperties;
import com.lakeserl.notification_service.entity.UserContact;
import com.lakeserl.notification_service.event.payload.RecipientContext;
import com.lakeserl.notification_service.repository.UserContactRepository;

import lombok.RequiredArgsConstructor;

/**
 * Owns the local {@code user_contacts} cache. Populated from user.* Kafka
 * events so email notifications for order/payment events (which carry no email)
 * can be addressed without calling user-service.
 */
@Service
@RequiredArgsConstructor
public class UserContactService {

    private final UserContactRepository userContactRepository;
    private final NotificationProperties props;

    @Transactional
    public void upsert(UUID userId, String email, String fullName, String locale) {
        if (userId == null || email == null || email.isBlank()) {
            return;
        }
        UserContact contact = userContactRepository.findById(userId)
                .orElseGet(() -> UserContact.builder().userId(userId).build());
        contact.setEmail(email);
        if (fullName != null && !fullName.isBlank()) {
            contact.setFullName(fullName);
        }
        if (locale != null && !locale.isBlank()) {
            contact.setLocale(locale);
        } else if (contact.getLocale() == null) {
            contact.setLocale(props.defaultLocale());
        }
        userContactRepository.save(contact);
    }

    public Optional<UserContact> find(UUID userId) {
        return userId == null ? Optional.empty() : userContactRepository.findById(userId);
    }

    /**
     * Builds the delivery context for a user. {@code emailOverride}, when set
     * (events that already carry an email), wins over the cached contact.
     */
    public RecipientContext resolveRecipient(UUID userId, String emailOverride) {
        Optional<UserContact> contact = find(userId);
        String email = (emailOverride != null && !emailOverride.isBlank())
                ? emailOverride
                : contact.map(UserContact::getEmail).orElse(null);
        String fullName = contact.map(UserContact::getFullName).orElse(null);
        String locale = contact.map(UserContact::getLocale).orElse(props.defaultLocale());
        return new RecipientContext(userId, email, fullName, locale);
    }
}
