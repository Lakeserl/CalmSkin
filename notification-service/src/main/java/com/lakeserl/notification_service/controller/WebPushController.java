package com.lakeserl.notification_service.controller;

import java.util.UUID;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.lakeserl.notification_service.config.properties.WebPushProperties;
import com.lakeserl.notification_service.dto.request.WebPushSubscribeRequest;
import com.lakeserl.notification_service.dto.request.WebPushUnsubscribeRequest;
import com.lakeserl.notification_service.dto.response.ApiResponse;
import com.lakeserl.notification_service.dto.response.VapidKeyResponse;
import com.lakeserl.notification_service.service.WebPushSubscriptionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** Browser Web Push (VAPID) subscription management. */
@RestController
@RequestMapping("/api/v1/notifications/web-push")
@RequiredArgsConstructor
@Tag(name = "Web Push", description = "VAPID subscription endpoints")
public class WebPushController {

    private final WebPushSubscriptionService subscriptionService;
    private final WebPushProperties webPushProperties;

    @GetMapping("/vapid-public-key")
    @Operation(summary = "Public VAPID key for pushManager.subscribe()")
    public ApiResponse<VapidKeyResponse> vapidPublicKey() {
        return ApiResponse.ok(new VapidKeyResponse(webPushProperties.vapidPublicKey()));
    }

    @PostMapping("/subscribe")
    @Operation(summary = "Register a browser push subscription")
    public ApiResponse<Void> subscribe(@RequestHeader("X-User-Id") UUID userId,
                                       @Valid @RequestBody WebPushSubscribeRequest request) {
        subscriptionService.subscribe(userId, request);
        return ApiResponse.ok("Subscription registered", null);
    }

    @DeleteMapping("/subscribe")
    @Operation(summary = "Remove a browser push subscription")
    public ApiResponse<Void> unsubscribe(@RequestHeader("X-User-Id") UUID userId,
                                         @Valid @RequestBody WebPushUnsubscribeRequest request) {
        subscriptionService.unsubscribe(userId, request.endpoint());
        return ApiResponse.ok("Subscription removed", null);
    }
}
