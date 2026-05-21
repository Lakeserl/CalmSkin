package com.lakeserl.notification_service.service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lakeserl.notification_service.config.properties.NotificationProperties;
import com.lakeserl.notification_service.config.properties.WebPushProperties;
import com.lakeserl.notification_service.entity.Notification;
import com.lakeserl.notification_service.enums.NotificationChannel;
import com.lakeserl.notification_service.enums.NotificationStatus;
import com.lakeserl.notification_service.event.payload.NotificationCommand;
import com.lakeserl.notification_service.event.payload.RecipientContext;
import com.lakeserl.notification_service.repository.NotificationRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Turns a {@link NotificationCommand} into persisted notification rows after
 * applying the user's preferences and quiet hours. Returns the ids of rows that
 * must be delivered immediately (EMAIL / WEB_PUSH); IN_APP rows are completed
 * inline, scheduled rows are left for the sender scheduler.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationDispatcher {

    private static final DateTimeFormatter HHMM = DateTimeFormatter.ofPattern("HH:mm");

    private final PreferenceService preferenceService;
    private final UserContactService userContactService;
    private final TemplateService templateService;
    private final UnreadCountService unreadCountService;
    private final NotificationRepository notificationRepository;
    private final WebPushProperties webPushProps;
    private final NotificationProperties props;
    private final ObjectMapper objectMapper;

    /**
     * Persists the notification rows for one command.
     *
     * @return ids of rows that need immediate channel delivery.
     */
    @Transactional
    public List<Long> dispatch(NotificationCommand command) {
        List<Long> immediate = new ArrayList<>();
        if (command.channels() == null || command.channels().isEmpty()) {
            log.warn("Command {} has no channels, nothing to dispatch", command.dedupKey());
            return immediate;
        }

        PreferenceView pref = preferenceService.resolve(command.userId());
        if (!pref.allows(command.category())) {
            log.debug("User {} opted out of category {}, skipping", command.userId(), command.category());
            return immediate;
        }

        RecipientContext recipient = userContactService.resolveRecipient(command.userId(), null);
        LocalDateTime now = LocalDateTime.now();
        boolean future = command.scheduledAt() != null && command.scheduledAt().isAfter(now);
        boolean quiet = !command.priority().bypassesQuietHours() && isQuietNow(pref);
        String metadata = toJson(command);

        for (NotificationChannel channel : command.channels()) {
            if (!pref.allows(channel)) {
                continue;
            }
            if (channel == NotificationChannel.EMAIL && !recipient.hasEmail()) {
                log.warn("No email on file for user {}, skipping email channel", command.userId());
                continue;
            }
            if (channel == NotificationChannel.WEB_PUSH && !webPushProps.isConfigured()) {
                continue;
            }
            if (quiet && channel != NotificationChannel.IN_APP) {
                log.debug("Quiet hours active for user {}, skipping {}", command.userId(), channel);
                continue;
            }

            TemplateService.RenderedContent content = templateService.render(
                    command.templateCode(), channel, recipient.locale(),
                    command.variables(), command.title(), command.body());

            boolean inApp = channel == NotificationChannel.IN_APP;
            NotificationStatus status = future
                    ? NotificationStatus.SCHEDULED
                    : (inApp ? NotificationStatus.SENT : NotificationStatus.PENDING);

            Notification row = Notification.builder()
                    .userId(command.userId())
                    .channel(channel)
                    .templateCode(command.templateCode())
                    .category(command.category())
                    .priority(command.priority())
                    .subject(content.subject())
                    .body(content.body())
                    .referenceId(command.referenceId())
                    .referenceType(command.referenceType())
                    .metadata(metadata)
                    .status(status)
                    .scheduledAt(future ? command.scheduledAt() : null)
                    .sentAt(!future && inApp ? now : null)
                    .retryCount(0)
                    .build();
            row = notificationRepository.save(row);

            if (!future && inApp) {
                unreadCountService.increment(command.userId());
            } else if (!future) {
                immediate.add(row.getId());
            }
        }
        return immediate;
    }

    private boolean isQuietNow(PreferenceView pref) {
        if (pref.quietHoursStart() == null || pref.quietHoursEnd() == null
                || pref.quietHoursStart().isBlank() || pref.quietHoursEnd().isBlank()) {
            return false;
        }
        LocalTime start = LocalTime.parse(pref.quietHoursStart(), HHMM);
        LocalTime end = LocalTime.parse(pref.quietHoursEnd(), HHMM);
        if (start.equals(end)) {
            return false;
        }
        LocalTime nowLocal = LocalTime.now(ZoneOffset.of(props.quietZoneOffset()));
        if (start.isBefore(end)) {
            return !nowLocal.isBefore(start) && nowLocal.isBefore(end);
        }
        // Window wraps past midnight, e.g. 22:00 -> 07:00.
        return !nowLocal.isBefore(start) || nowLocal.isBefore(end);
    }

    private String toJson(NotificationCommand command) {
        if (command.variables() == null || command.variables().isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(command.variables());
        } catch (Exception ex) {
            log.warn("Failed to serialise notification metadata", ex);
            return null;
        }
    }
}
