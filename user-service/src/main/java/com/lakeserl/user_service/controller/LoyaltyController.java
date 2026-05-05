package com.lakeserl.user_service.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.lakeserl.user_service.model.dto.response.ApiResponse;
import com.lakeserl.user_service.model.entity.PointTransaction;
import com.lakeserl.user_service.model.entity.UserPoint;
import com.lakeserl.user_service.security.userDetails.CustomUserDetails;
import com.lakeserl.user_service.service.LoyaltyService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/users/me/points")
@RequiredArgsConstructor
@Tag(name = "Loyalty", description = "Loyalty points APIs")
public class LoyaltyController {

    private final LoyaltyService loyaltyService;

    @GetMapping
    @Operation(summary = "Get current points and tier")
    public ResponseEntity<ApiResponse<UserPoint>> getPoints(
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(ApiResponse.ok(loyaltyService.getPoints(principal.getId())));
    }

    @GetMapping("/transactions")
    @Operation(summary = "Get point transaction history")
    public ResponseEntity<ApiResponse<Page<PointTransaction>>> getTransactions(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        size = Math.min(size, 100);
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(ApiResponse.ok(loyaltyService.getTransactions(principal.getId(), pageable)));
    }
}
