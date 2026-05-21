package com.lakeserl.notification_service.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.lakeserl.notification_service.dto.request.BroadcastRequest;
import com.lakeserl.notification_service.dto.request.UpdateTemplateRequest;
import com.lakeserl.notification_service.dto.response.ApiResponse;
import com.lakeserl.notification_service.dto.response.NotificationResponse;
import com.lakeserl.notification_service.dto.response.NotificationStatsResponse;
import com.lakeserl.notification_service.dto.response.TemplateResponse;
import com.lakeserl.notification_service.service.AdminNotificationService;
import com.lakeserl.notification_service.service.NotificationService;
import com.lakeserl.notification_service.support.NotificationConstants;
import com.lakeserl.notification_service.support.Pages;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** Admin operations: stats, broadcast, templates and the staff alert feed. */
@RestController
@RequestMapping("/api/v1/admin/notifications")
@RequiredArgsConstructor
@Tag(name = "Admin notifications", description = "Stats, broadcast and template management")
public class AdminNotificationController {

    private final AdminNotificationService adminNotificationService;
    private final NotificationService notificationService;

    @GetMapping
    @Operation(summary = "List the shared staff alert feed (low stock, new reviews)")
    public ApiResponse<List<NotificationResponse>> adminFeed(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status) {
        Page<NotificationResponse> result = notificationService.list(
                NotificationConstants.ADMIN_FEED_USER_ID, status, PageRequest.of(page, size));
        return ApiResponse.ok("Admin notifications retrieved", result.getContent(), Pages.info(result));
    }

    @GetMapping("/stats")
    @Operation(summary = "Daily delivery counters")
    public ApiResponse<NotificationStatsResponse> stats() {
        return ApiResponse.ok(adminNotificationService.stats());
    }

    @PostMapping("/broadcast")
    @Operation(summary = "Send one template to a set of users")
    public ApiResponse<Void> broadcast(@Valid @RequestBody BroadcastRequest request) {
        int queued = adminNotificationService.broadcast(request);
        return ApiResponse.ok("Broadcast queued for " + queued + " users", null);
    }

    @GetMapping("/templates")
    @Operation(summary = "List notification templates")
    public ApiResponse<List<TemplateResponse>> templates(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<TemplateResponse> result = adminNotificationService.listTemplates(PageRequest.of(page, size));
        return ApiResponse.ok("Templates retrieved", result.getContent(), Pages.info(result));
    }

    @PutMapping("/templates/{id}")
    @Operation(summary = "Update a notification template")
    public ApiResponse<TemplateResponse> updateTemplate(@PathVariable Long id,
                                                        @RequestBody UpdateTemplateRequest request) {
        return ApiResponse.ok("Template updated", adminNotificationService.updateTemplate(id, request));
    }
}
