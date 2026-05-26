package com.lakeserl.subscription_service.controller;

import com.lakeserl.subscription_service.dto.request.CreateSubscriptionRequest;
import com.lakeserl.subscription_service.dto.request.UpdateSubscriptionRequest;
import com.lakeserl.subscription_service.dto.response.ApiResponse;
import com.lakeserl.subscription_service.dto.response.ApiResponse.PageInfo;
import com.lakeserl.subscription_service.dto.response.SubscriptionDTO;
import com.lakeserl.subscription_service.service.SubscriptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Customer-facing subscription endpoints.
 * All write operations are ownership-scoped: the caller can only manage their own subscriptions.
 */
@RestController
@RequestMapping("/api/v1/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<SubscriptionDTO> create(
            @Valid @RequestBody CreateSubscriptionRequest request,
            Authentication auth) {
        UUID userId = parseUserId(auth);
        return ApiResponse.ok("Subscription created", subscriptionService.create(userId, request));
    }

    @GetMapping
    public ApiResponse<List<SubscriptionDTO>> list(Authentication auth, Pageable pageable) {
        UUID userId = parseUserId(auth);
        Page<SubscriptionDTO> page = subscriptionService.listByUser(userId, pageable);
        return ApiResponse.ok("OK", page.getContent(), PageInfo.builder()
                .currentPage(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .hasNext(page.hasNext())
                .hasPrevious(page.hasPrevious())
                .build());
    }

    @GetMapping("/{id}")
    public ApiResponse<SubscriptionDTO> getById(
            @PathVariable UUID id,
            Authentication auth) {
        return ApiResponse.ok(subscriptionService.getById(id, parseUserId(auth)));
    }

    @PutMapping("/{id}")
    public ApiResponse<SubscriptionDTO> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateSubscriptionRequest request,
            Authentication auth) {
        return ApiResponse.ok("Subscription updated",
                subscriptionService.update(id, parseUserId(auth), request));
    }

    @PutMapping("/{id}/pause")
    public ApiResponse<SubscriptionDTO> pause(
            @PathVariable UUID id,
            Authentication auth) {
        return ApiResponse.ok("Subscription paused",
                subscriptionService.pause(id, parseUserId(auth)));
    }

    @PutMapping("/{id}/resume")
    public ApiResponse<SubscriptionDTO> resume(
            @PathVariable UUID id,
            Authentication auth) {
        return ApiResponse.ok("Subscription resumed",
                subscriptionService.resume(id, parseUserId(auth)));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<SubscriptionDTO> cancel(
            @PathVariable UUID id,
            Authentication auth) {
        return ApiResponse.ok("Subscription cancelled",
                subscriptionService.cancel(id, parseUserId(auth)));
    }

    private UUID parseUserId(Authentication auth) {
        return UUID.fromString((String) auth.getPrincipal());
    }
}
