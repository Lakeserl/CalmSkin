package com.lakeserl.product_service.service;

import com.lakeserl.product_service.dto.response.PresignResponse;
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
 * Generates pre-signed S3 PUT URLs for product image uploads (admin-only).
 *
 * Security:
 * - Key generated server-side as  products/{productId}/{UUID}.{ext}
 * - Extension derived from contentType, never from client filename
 * - Max size 10 MB for product images
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductMediaPresignService {

    private static final long MAX_SIZE_BYTES = 10 * 1024 * 1024L; // 10 MB for product images

    private final S3Presigner s3Presigner;

    @Value("${app.s3.bucket:calmskin-product-media}")
    private String bucket;

    @Value("${app.s3.cdn-base-url:}")
    private String cdnBaseUrl;

    public PresignResponse presign(Long productId, String filename, String contentType, long sizeBytes) {
        if (sizeBytes > MAX_SIZE_BYTES) {
            throw new IllegalArgumentException("File size must be under 10 MB (declared: " + sizeBytes + " bytes)");
        }
        validateContentType(contentType);

        String ext = contentTypeToExtension(contentType);
        String key = "products/" + productId + "/" + UUID.randomUUID() + "." + ext;

        PresignedPutObjectRequest presigned = s3Presigner.presignPutObject(b -> b
                .signatureDuration(Duration.ofMinutes(15))
                .putObjectRequest(PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .contentType(contentType)
                        .build()));

        String mediaUrl = (cdnBaseUrl == null || cdnBaseUrl.isBlank())
                ? "https://" + bucket + ".s3.amazonaws.com/" + key
                : cdnBaseUrl + "/" + key;

        log.debug("Presigned product image upload for productId={} → key={}", productId, key);

        return PresignResponse.builder()
                .uploadUrl(presigned.url().toString())
                .mediaUrl(mediaUrl)
                .build();
    }

    private void validateContentType(String contentType) {
        if (!contentType.equals("image/jpeg") && !contentType.equals("image/png") && !contentType.equals("image/webp")) {
            throw new IllegalArgumentException("Only image/jpeg, image/png, image/webp allowed for product images");
        }
    }

    private String contentTypeToExtension(String contentType) {
        return switch (contentType) {
            case "image/jpeg" -> "jpg";
            case "image/png"  -> "png";
            case "image/webp" -> "webp";
            default           -> "jpg";
        };
    }
}
