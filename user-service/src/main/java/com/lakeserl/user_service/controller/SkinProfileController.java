package com.lakeserl.user_service.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.lakeserl.user_service.model.dto.request.SkinProfileRequest;
import com.lakeserl.user_service.model.dto.response.ApiResponse;
import com.lakeserl.user_service.model.dto.response.SkinProfileResponse;
import com.lakeserl.user_service.security.userDetails.CustomUserDetails;
import com.lakeserl.user_service.service.SkinProfileService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/users/me/skin-profile")
@RequiredArgsConstructor
@Tag(name = "Skin Profile", description = "Skin profile management APIs")
public class SkinProfileController {

    private final SkinProfileService skinProfileService;

    @GetMapping
    @Operation(summary = "Get skin profile")
    public ResponseEntity<ApiResponse<SkinProfileResponse>> getProfile(
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(ApiResponse.ok(skinProfileService.getProfile(principal.getId())));
    }

    @PostMapping
    @Operation(summary = "Create skin profile")
    public ResponseEntity<ApiResponse<SkinProfileResponse>> createProfile(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody SkinProfileRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(skinProfileService.createProfile(principal.getId(), request)));
    }

    @PutMapping
    @Operation(summary = "Update skin profile")
    public ResponseEntity<ApiResponse<SkinProfileResponse>> updateProfile(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody SkinProfileRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(skinProfileService.updateProfile(principal.getId(), request)));
    }
}
