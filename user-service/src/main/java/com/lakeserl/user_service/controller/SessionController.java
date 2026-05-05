package com.lakeserl.user_service.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.lakeserl.user_service.model.dto.response.ApiResponse;
import com.lakeserl.user_service.model.entity.RefreshToken;
import com.lakeserl.user_service.repository.RefreshTokenRepository;
import com.lakeserl.user_service.security.RedisTokenService;
import com.lakeserl.user_service.security.userDetails.CustomUserDetails;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/users/me/sessions")
@RequiredArgsConstructor
@Tag(name = "Session", description = "Session management APIs")
public class SessionController {

    private final RefreshTokenRepository refreshTokenRepository;
    private final RedisTokenService redisTokenService;

    @GetMapping
    @Operation(summary = "Get active sessions")
    public ResponseEntity<ApiResponse<List<SessionInfo>>> getSessions(
            @AuthenticationPrincipal CustomUserDetails principal) {
        List<RefreshToken> tokens = refreshTokenRepository.findByUserIdAndRevokedFalse(principal.getId());
        List<SessionInfo> sessions = tokens.stream()
                .map(t -> new SessionInfo(t.getId(), t.getDeviceInfo(), t.getIpAddress(), t.getCreatedAt().toString()))
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(sessions));
    }

    @DeleteMapping("/{sessionId}")
    @Operation(summary = "Revoke a specific session")
    public ResponseEntity<ApiResponse<Void>> revokeSession(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable UUID sessionId) {
        refreshTokenRepository.findById(sessionId).ifPresent(rt -> {
            if (rt.getUser().getId().equals(principal.getId()) && !rt.isRevoked()) {
                rt.setRevoked(true);
                refreshTokenRepository.save(rt);
                redisTokenService.revokeRefreshToken(principal.getId().toString(), rt.getToken());
            }
        });
        return ResponseEntity.ok(ApiResponse.ok("Session revoked"));
    }

    public record SessionInfo(UUID id, String deviceInfo, String ipAddress, String createdAt) {}
}
