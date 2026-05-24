package com.lakeserl.product_service.controller;

import com.lakeserl.product_service.dto.response.ApiResponse;
import com.lakeserl.product_service.dto.response.PresignResponse;
import com.lakeserl.product_service.service.ProductMediaPresignService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * Admin-only endpoint for generating pre-signed S3 URLs for product image uploads.
 *
 * Upload flow:
 *   1. POST /api/v1/admin/products/media/presign?productId=123&filename=...&contentType=...&sizeBytes=...
 *      → returns {uploadUrl, mediaUrl}
 *   2. Admin FE: PUT uploadUrl with image bytes (direct to S3)
 *   3. Admin creates/updates product with imageUrl = mediaUrl
 */
@RestController
@RequestMapping("/api/v1/admin/products/media")
@RequiredArgsConstructor
@Validated
@Tag(name = "Admin Product Media", description = "Pre-signed S3 URL generation for product images (admin only)")
public class ProductMediaController {

    private final ProductMediaPresignService productMediaPresignService;

    @PostMapping("/presign")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Get a pre-signed S3 URL for product image upload (admin only)",
        description = "Returns an uploadUrl (PUT directly to S3) and a mediaUrl to use as the product image URL. " +
                      "Key is server-generated — original filename is never used in the S3 path."
    )
    public ApiResponse<PresignResponse> presign(
            @RequestParam @NotNull Long productId,
            @RequestParam @NotBlank String filename,
            @RequestParam @NotBlank
            @Pattern(regexp = "image/(jpeg|png|webp)", message = "Only image/jpeg, image/png, image/webp are allowed")
            String contentType,
            @RequestParam @Positive long sizeBytes) {
        return ApiResponse.ok(productMediaPresignService.presign(productId, filename, contentType, sizeBytes));
    }
}
