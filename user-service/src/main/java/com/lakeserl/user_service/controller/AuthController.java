package com.lakeserl.user_service.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.lakeserl.user_service.model.dto.request.*;
import com.lakeserl.user_service.model.dto.response.ApiResponse;
import com.lakeserl.user_service.model.dto.response.AuthResponse;
import com.lakeserl.user_service.security.userDetails.CustomUserDetails;
import com.lakeserl.user_service.service.AuthService;
import com.lakeserl.user_service.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Auth APIs: register, login, logout, token refresh, OTP, password reset")
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    @PostMapping("/register")
    @Operation(summary = "Register new user")
    public ResponseEntity<ApiResponse<Void>> register(@Valid @RequestBody Register request) {
        authService.register(request);
        return ResponseEntity.ok(ApiResponse.ok("Registration successful. Check email for OTP."));
    }

    @PostMapping("/verify-email")
    @Operation(summary = "Verify email with OTP")
    public ResponseEntity<ApiResponse<Void>> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        authService.verifyEmail(request.getEmail(), request.getOtp());
        return ResponseEntity.ok(ApiResponse.ok("Email verified successfully"));
    }

    @PostMapping("/resend-verification")
    @Operation(summary = "Resend email verification OTP")
    public ResponseEntity<ApiResponse<Void>> resendVerification(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.resendVerification(request.getEmail());
        return ResponseEntity.ok(ApiResponse.ok("Verification OTP sent"));
    }

    @PostMapping("/login")
    @Operation(summary = "Login with email & password")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody Login request, HttpServletRequest httpRequest) {
        String ip = httpRequest.getRemoteAddr();
        String device = httpRequest.getHeader("User-Agent");
        AuthResponse response = authService.login(request, ip, device);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/login/otp")
    @Operation(summary = "Login with phone OTP")
    public ResponseEntity<ApiResponse<AuthResponse>> loginWithOtp(
            @Valid @RequestBody OtpLoginRequest request, HttpServletRequest httpRequest) {
        String ip = httpRequest.getRemoteAddr();
        String device = httpRequest.getHeader("User-Agent");
        AuthResponse response = authService.loginWithOtp(request.getPhoneNumber(), request.getOtp(), ip, device);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/refresh-token")
    @Operation(summary = "Refresh access token (token rotation)")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        AuthResponse response = authService.refreshToken(request.getRefreshToken());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout current session")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletRequest request) {
        String token = extractToken(request);
        authService.logout(token);
        return ResponseEntity.ok(ApiResponse.ok("Logged out"));
    }

    @PostMapping("/logout-all")
    @Operation(summary = "Logout all sessions")
    public ResponseEntity<ApiResponse<Void>> logoutAll(HttpServletRequest request) {
        String token = extractToken(request);
        authService.logoutAll(token);
        return ResponseEntity.ok(ApiResponse.ok("All sessions logged out"));
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Request password reset OTP")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request.getEmail());
        return ResponseEntity.ok(ApiResponse.ok("Password reset OTP sent"));
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset password with OTP")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.ok("Password reset successfully"));
    }

    @PostMapping("/change-password")
    @Operation(summary = "Change password (requires auth)")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(principal.getId(), request);
        return ResponseEntity.ok(ApiResponse.ok("Password changed successfully"));
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        throw new IllegalArgumentException("Missing Authorization header");
    }
}
