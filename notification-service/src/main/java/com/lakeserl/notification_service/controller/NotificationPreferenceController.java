package com.lakeserl.notification_service.controller;

import java.time.format.DateTimeFormatter;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.lakeserl.notification_service.dto.request.UpdatePreferenceRequest;
import com.lakeserl.notification_service.dto.response.ApiResponse;
import com.lakeserl.notification_service.dto.response.PreferenceResponse;
import com.lakeserl.notification_service.entity.NotificationPreference;
import com.lakeserl.notification_service.service.PreferenceService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/** Per-user notification preference management. */
@RestController
@RequestMapping("/api/v1/notifications/preferences")
@RequiredArgsConstructor
@Tag(name = "Notification preferences", description = "Channel, category and quiet-hour settings")
public class NotificationPreferenceController {

    private static final DateTimeFormatter HHMM = DateTimeFormatter.ofPattern("HH:mm");

    private final PreferenceService preferenceService;

    @GetMapping
    @Operation(summary = "Get the caller's notification preferences")
    public ApiResponse<PreferenceResponse> get(@RequestHeader("X-User-Id") UUID userId) {
        return ApiResponse.ok(toResponse(preferenceService.getOrCreate(userId)));
    }

    @PutMapping
    @Operation(summary = "Update the caller's notification preferences")
    public ApiResponse<PreferenceResponse> update(@RequestHeader("X-User-Id") UUID userId,
                                                  @RequestBody UpdatePreferenceRequest request) {
        return ApiResponse.ok("Preferences updated", toResponse(preferenceService.update(userId, request)));
    }

    private PreferenceResponse toResponse(NotificationPreference p) {
        return new PreferenceResponse(
                p.getUserId(),
                Boolean.TRUE.equals(p.getEmailEnabled()),
                Boolean.TRUE.equals(p.getWebPushEnabled()),
                Boolean.TRUE.equals(p.getInAppEnabled()),
                Boolean.TRUE.equals(p.getOrderUpdates()),
                Boolean.TRUE.equals(p.getPromotions()),
                Boolean.TRUE.equals(p.getReviews()),
                Boolean.TRUE.equals(p.getStockAlerts()),
                Boolean.TRUE.equals(p.getSecurityAlerts()),
                p.getQuietHoursStart() == null ? null : p.getQuietHoursStart().format(HHMM),
                p.getQuietHoursEnd() == null ? null : p.getQuietHoursEnd().format(HHMM),
                p.getLocale());
    }
}
