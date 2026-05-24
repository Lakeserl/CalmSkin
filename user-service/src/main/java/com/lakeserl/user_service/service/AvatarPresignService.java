package com.lakeserl.user_service.service;

import com.lakeserl.user_service.exception.InvalidFileException;
import com.lakeserl.user_service.model.dto.request.AvatarPresignRequest;
import com.lakeserl.user_service.model.dto.response.PresignResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;

import java.time.Duration;
import java.util.UUID;

/**
 * Generates pre-signed S3 PUT URLs for avatar uploads.
 *
 * Security design:
 * - Object key is always generated server-side as  avatars/{userId}/{UUID}.{ext}
 *   where ext is derived from the declared contentType — NEVER from the client filename.
 *   This prevents path-traversal attacks (audit item C5).
 * - Size limit is enforced at the declared sizeBytes field level (5 MB).
 * - MIME type validation on the actual upload is the client's responsibility
 *   (the presign URL sets ContentType so S3 enforces it at upload time).
 *
 * Flow:
 *   FE → POST /api/v1/users/me/avatar/presign → gets {uploadUrl, mediaUrl}
 *   FE → PUT uploadUrl (with file bytes) → S3
 *   FE → PATCH /api/v1/users/me { avatarUrl: mediaUrl } → saves URL
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AvatarPresignService {

    private static final long MAX_SIZE_BYTES = 5 * 1024 * 1024L; // 5 MB

    private final S3Presigner s3Presigner;

    @Value("${app.s3.bucket:calmskin-avatars}")
    private String bucket;

    @Value("${app.s3.cdn-base-url:}")
    private String cdnBaseUrl;

    public PresignResponse presign(UUID userId, AvatarPresignRequest request) {
        if (request.getSizeBytes() > MAX_SIZE_BYTES) {
            throw new InvalidFileException("File size must be under 5 MB (declared: " + request.getSizeBytes() + " bytes)");
        }

        // Derive extension from the declared contentType — never trust the filename
        String ext = contentTypeToExtension(request.getContentType());
        String key = "avatars/" + userId + "/" + UUID.randomUUID() + "." + ext;

        PresignedPutObjectRequest presigned = s3Presigner.presignPutObject(b -> b
                .signatureDuration(Duration.ofMinutes(10))
                .putObjectRequest(PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .contentType(request.getContentType())
                        .build()));

        String mediaUrl = (cdnBaseUrl == null || cdnBaseUrl.isBlank())
                ? "https://" + bucket + ".s3.amazonaws.com/" + key
                : cdnBaseUrl + "/" + key;

        log.debug("Presigned avatar upload for userId={} → key={}", userId, key);

        return new PresignResponse(presigned.url().toString(), mediaUrl);
    }

    private String contentTypeToExtension(String contentType) {
        return switch (contentType) {
            case "image/jpeg" -> "jpg";
            case "image/png"  -> "png";
            case "image/webp" -> "webp";
            default           -> "jpg"; // fallback; validation annotation already enforces allowed types
        };
    }
}
