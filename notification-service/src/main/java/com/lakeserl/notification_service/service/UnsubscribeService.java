package com.lakeserl.notification_service.service;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lakeserl.notification_service.entity.NotificationUnsubscribeToken;
import com.lakeserl.notification_service.exception.BadRequestException;
import com.lakeserl.notification_service.exception.ResourceNotFoundException;
import com.lakeserl.notification_service.repository.NotificationUnsubscribeTokenRepository;

import lombok.RequiredArgsConstructor;

/**
 * Issues and redeems one-click email unsubscribe tokens. Redeeming a token
 * turns off the matching preference category without requiring a login.
 */
@Service
@RequiredArgsConstructor
public class UnsubscribeService {

    private static final int TOKEN_VALIDITY_DAYS = 30;

    private final NotificationUnsubscribeTokenRepository tokenRepository;
    private final PreferenceService preferenceService;

    @Transactional
    public String issueToken(UUID userId, String category) {
        String token = UUID.randomUUID().toString().replace("-", "");
        tokenRepository.save(NotificationUnsubscribeToken.builder()
                .token(token)
                .userId(userId)
                .category(category)
                .expiresAt(LocalDateTime.now().plusDays(TOKEN_VALIDITY_DAYS))
                .build());
        return token;
    }

    /**
     * Redeems a token and returns the category that was unsubscribed. Redeeming
     * an already-used token is a no-op so a double click is harmless.
     */
    @Transactional
    public String consume(String token) {
        NotificationUnsubscribeToken entry = tokenRepository.findById(token)
                .orElseThrow(() -> new ResourceNotFoundException("Unsubscribe link is invalid"));

        if (entry.getUsedAt() != null) {
            return entry.getCategory();
        }
        if (entry.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Unsubscribe link has expired");
        }

        preferenceService.disableCategory(entry.getUserId(), entry.getCategory());
        entry.setUsedAt(LocalDateTime.now());
        tokenRepository.save(entry);
        return entry.getCategory();
    }
}
