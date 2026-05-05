package com.lakeserl.user_service.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.lakeserl.user_service.model.dto.UserDTO;
import com.lakeserl.user_service.model.dto.request.ChangePasswordRequest;
import com.lakeserl.user_service.model.dto.request.UpdateProfileRequest;
import com.lakeserl.user_service.model.dto.response.ApiResponse;
import com.lakeserl.user_service.security.userDetails.CustomUserDetails;
import com.lakeserl.user_service.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "User Profile", description = "User profile management APIs")
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    @Operation(summary = "Get current user profile")
    public ResponseEntity<ApiResponse<UserDTO>> getProfile(
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(ApiResponse.ok(userService.getProfile(principal.getId())));
    }

    @PutMapping("/me")
    @Operation(summary = "Update current user profile")
    public ResponseEntity<ApiResponse<UserDTO>> updateProfile(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(userService.updateProfile(principal.getId(), request)));
    }

    @PostMapping("/me/change-password")
    @Operation(summary = "Change password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(principal.getId(), request);
        return ResponseEntity.ok(ApiResponse.ok("Password changed successfully"));
    }

    @PatchMapping("/me/deactivate")
    @Operation(summary = "Deactivate account")
    public ResponseEntity<ApiResponse<Void>> deactivateAccount(
            @AuthenticationPrincipal CustomUserDetails principal) {
        userService.deactivateAccount(principal.getId());
        return ResponseEntity.ok(ApiResponse.ok("Account deactivated"));
    }

    @DeleteMapping("/me/account")
    @Operation(summary = "Delete account permanently")
    public ResponseEntity<ApiResponse<Void>> deleteAccount(
            @AuthenticationPrincipal CustomUserDetails principal) {
        userService.deleteAccount(principal.getId());
        return ResponseEntity.ok(ApiResponse.ok("Account deleted"));
    }

    @PostMapping("/me/avatar")
    @Operation(summary = "Upload avatar")
    public ResponseEntity<ApiResponse<String>> uploadAvatar(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        String url = userService.uploadAvatar(principal.getId(), file);
        return ResponseEntity.ok(ApiResponse.ok("Avatar uploaded", url));
    }

    @DeleteMapping("/me/avatar")
    @Operation(summary = "Delete avatar")
    public ResponseEntity<ApiResponse<Void>> deleteAvatar(
            @AuthenticationPrincipal CustomUserDetails principal) {
        userService.deleteAvatar(principal.getId());
        return ResponseEntity.ok(ApiResponse.ok("Avatar deleted"));
    }
}
