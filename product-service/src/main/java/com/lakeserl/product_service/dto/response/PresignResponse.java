package com.lakeserl.product_service.dto.response;

import lombok.Builder;
import lombok.Data;

/**
 * Response contract for pre-signed S3 upload URL generation.
 * Mirrors review-service PresignResponse so FE can reuse a single uploader component.
 *
 * uploadUrl: PUT this URL with file bytes + Content-Type header directly to S3.
 * mediaUrl:  The final public/CDN URL to save in the product record after upload.
 */
@Data
@Builder
public class PresignResponse {
    private String uploadUrl;
    private String mediaUrl;
}
