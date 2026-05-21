package com.lakeserl.notification_service.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.lakeserl.notification_service.dto.response.ApiResponse;
import com.lakeserl.notification_service.dto.response.NotificationResponse;
import com.lakeserl.notification_service.dto.response.UnreadCountResponse;
import com.lakeserl.notification_service.service.NotificationService;
import com.lakeserl.notification_service.support.Pages;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/** User-facing in-app notification feed. */
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "In-app notification feed")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/me")
    @Operation(summary = "List the caller's in-app notifications")
    public ApiResponse<List<NotificationResponse>> list(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status) {
        Page<NotificationResponse> result =
                notificationService.list(userId, status, PageRequest.of(page, size));
        return ApiResponse.ok("Notifications retrieved", result.getContent(), Pages.info(result));
    }

    @GetMapping("/me/unread-count")
    @Operation(summary = "Unread badge count")
    public ApiResponse<UnreadCountResponse> unreadCount(@RequestHeader("X-User-Id") UUID userId) {
        return ApiResponse.ok(new UnreadCountResponse(notificationService.unreadCount(userId)));
    }

    @PatchMapping("/{id}/read")
    @Operation(summary = "Mark one notification as read")
    public ApiResponse<Void> markRead(@RequestHeader("X-User-Id") UUID userId,
                                      @PathVariable Long id) {
        notificationService.markRead(userId, id);
        return ApiResponse.ok("Notification marked read", null);
    }

    @PatchMapping("/read-all")
    @Operation(summary = "Mark every notification as read")
    public ApiResponse<Void> markAllRead(@RequestHeader("X-User-Id") UUID userId) {
        int updated = notificationService.markAllRead(userId);
        return ApiResponse.ok("Marked " + updated + " notifications read", null);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete one notification")
    public ApiResponse<Void> delete(@RequestHeader("X-User-Id") UUID userId,
                                    @PathVariable Long id) {
        notificationService.delete(userId, id);
        return ApiResponse.ok("Notification deleted", null);
    }
}
