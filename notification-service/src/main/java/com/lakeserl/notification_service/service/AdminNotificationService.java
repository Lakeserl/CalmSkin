package com.lakeserl.notification_service.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lakeserl.notification_service.dto.request.BroadcastRequest;
import com.lakeserl.notification_service.dto.request.UpdateTemplateRequest;
import com.lakeserl.notification_service.dto.response.NotificationStatsResponse;
import com.lakeserl.notification_service.dto.response.TemplateResponse;
import com.lakeserl.notification_service.entity.NotificationTemplate;
import com.lakeserl.notification_service.enums.NotificationCategory;
import com.lakeserl.notification_service.enums.NotificationChannel;
import com.lakeserl.notification_service.enums.NotificationPriority;
import com.lakeserl.notification_service.enums.NotificationStatus;
import com.lakeserl.notification_service.event.payload.NotificationCommand;
import com.lakeserl.notification_service.event.producer.NotificationCommandProducer;
import com.lakeserl.notification_service.exception.ResourceNotFoundException;
import com.lakeserl.notification_service.repository.NotificationRepository;
import com.lakeserl.notification_service.repository.NotificationTemplateRepository;

import lombok.RequiredArgsConstructor;

/**
 * Admin operations: daily stats, bulk broadcast and template maintenance.
 * A broadcast fans out one {@link NotificationCommand} per user onto the normal
 * priority lane so it never starves transactional traffic.
 */
@Service
@RequiredArgsConstructor
public class AdminNotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationTemplateRepository templateRepository;
    private final NotificationCommandProducer commandProducer;

    public NotificationStatsResponse stats() {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        return new NotificationStatsResponse(
                notificationRepository.countByStatusAndCreatedAtGreaterThanEqual(
                        NotificationStatus.SENT, startOfDay),
                notificationRepository.countByStatusAndCreatedAtGreaterThanEqual(
                        NotificationStatus.FAILED, startOfDay),
                notificationRepository.countByChannelAndCreatedAtGreaterThanEqual(
                        NotificationChannel.EMAIL, startOfDay),
                notificationRepository.countByChannelAndCreatedAtGreaterThanEqual(
                        NotificationChannel.WEB_PUSH, startOfDay),
                notificationRepository.countByChannelAndCreatedAtGreaterThanEqual(
                        NotificationChannel.IN_APP, startOfDay));
    }

    public int broadcast(BroadcastRequest request) {
        long batch = System.currentTimeMillis();
        for (UUID userId : request.userIds()) {
            NotificationCommand command = new NotificationCommand(
                    "broadcast:" + request.templateCode() + ":" + userId + ":" + batch,
                    userId,
                    NotificationCategory.PROMOTIONS,
                    NotificationPriority.NORMAL,
                    request.templateCode(),
                    List.of(NotificationChannel.IN_APP,
                            NotificationChannel.WEB_PUSH,
                            NotificationChannel.EMAIL),
                    request.title(),
                    request.body(),
                    null,
                    "BROADCAST",
                    Map.of(),
                    request.scheduledAt());
            commandProducer.publish(command);
        }
        return request.userIds().size();
    }

    public Page<TemplateResponse> listTemplates(Pageable pageable) {
        return templateRepository.findAll(pageable).map(this::toResponse);
    }

    @Transactional
    public TemplateResponse updateTemplate(Long id, UpdateTemplateRequest request) {
        NotificationTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Template not found: " + id));
        if (request.subject() != null) {
            template.setSubject(request.subject());
        }
        if (request.body() != null) {
            template.setBody(request.body());
        }
        if (request.variables() != null) {
            template.setVariables(request.variables());
        }
        if (request.isActive() != null) {
            template.setIsActive(request.isActive());
        }
        template.setVersion(template.getVersion() + 1);
        return toResponse(templateRepository.save(template));
    }

    private TemplateResponse toResponse(NotificationTemplate t) {
        return new TemplateResponse(
                t.getId(),
                t.getCode(),
                t.getChannel().name(),
                t.getLocale(),
                t.getSubject(),
                t.getBody(),
                t.getVariables(),
                Boolean.TRUE.equals(t.getIsActive()),
                t.getVersion(),
                t.getUpdatedAt());
    }
}
