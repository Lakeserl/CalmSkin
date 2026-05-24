package com.lakeserl.user_service.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.lakeserl.user_service.model.dto.request.RecentlyViewedRequest;
import com.lakeserl.user_service.model.dto.response.ApiResponse;
import com.lakeserl.user_service.security.userDetails.CustomUserDetails;
import com.lakeserl.user_service.service.RecentlyViewedService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/users/me/recently-viewed")
@RequiredArgsConstructor
@Tag(name = "Recently Viewed", description = "Per-user recently viewed product tracking")
public class RecentlyViewedController {

    private final RecentlyViewedService recentlyViewedService;

    @PostMapping
    @Operation(summary = "Record a product view (upserts viewed_at on conflict)")
    public ResponseEntity<ApiResponse<Void>> record(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody RecentlyViewedRequest request) {
        recentlyViewedService.record(principal.getId(), request.getProductId());
        return ResponseEntity.ok(ApiResponse.ok("Recorded"));
    }

    @GetMapping
    @Operation(summary = "List recently viewed product IDs (newest first)")
    public ResponseEntity<ApiResponse<List<Long>>> list(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(ApiResponse.ok(recentlyViewedService.list(principal.getId(), limit)));
    }
}
