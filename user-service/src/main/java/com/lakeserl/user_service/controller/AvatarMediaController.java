package com.lakeserl.user_service.controller;

import com.lakeserl.user_service.model.dto.request.AvatarPresignRequest;
import com.lakeserl.user_service.model.dto.response.ApiResponse;
import com.lakeserl.user_service.model.dto.response.PresignResponse;
import com.lakeserl.user_service.security.userDetails.CustomUserDetails;
import com.lakeserl.user_service.service.AvatarPresignService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Provides pre-signed S3 URL for avatar upload.
 *
 * Upload flow:
 *   1. POST /api/v1/users/me/avatar/presign  → {uploadUrl, mediaUrl}
 *   2. FE: PUT uploadUrl with image bytes (direct to S3, no server involvement)
 *   3. PATCH /api/v1/users/me { "avatarUrl": "<mediaUrl>" }  → saves the avatar URL
 *
 * This replaces the old direct multipart upload endpoint (UserServiceImpl.uploadAvatar)
 * which was vulnerable to path-traversal (audit C5). The old endpoint is retained for
 * backward compatibility but deprecated — use presign flow instead.
 */
@RestController
@RequestMapping("/api/v1/users/me/avatar")
@RequiredArgsConstructor
@Tag(name = "Avatar", description = "Avatar upload management")
public class AvatarMediaController {

    private final AvatarPresignService avatarPresignService;

    @PostMapping("/presign")
    @Operation(
        summary = "Get a pre-signed S3 URL to upload avatar",
        description = "Returns an uploadUrl (PUT directly to S3) and a mediaUrl to save after successful upload. " +
                      "Key is server-generated — original filename is never used in the S3 path."
    )
    public ResponseEntity<ApiResponse<PresignResponse>> presign(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody AvatarPresignRequest request) {
        PresignResponse response = avatarPresignService.presign(principal.getId(), request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
