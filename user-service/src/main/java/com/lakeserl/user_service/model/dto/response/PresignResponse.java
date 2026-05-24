package com.lakeserl.user_service.model.dto.response;

/**
 * Response contract for pre-signed URL endpoints.
 * Mirrors review-service PresignResponse for FE reuse of a single uploader component.
 *
 * uploadUrl: PUT this URL with file bytes + Content-Type header directly to S3.
 * mediaUrl:  The final public/CDN URL to save as avatarUrl after successful upload.
 */
public class PresignResponse {

    private String uploadUrl;
    private String mediaUrl;

    public PresignResponse() {}

    public PresignResponse(String uploadUrl, String mediaUrl) {
        this.uploadUrl = uploadUrl;
        this.mediaUrl = mediaUrl;
    }

    public String getUploadUrl() { return uploadUrl; }
    public void setUploadUrl(String uploadUrl) { this.uploadUrl = uploadUrl; }

    public String getMediaUrl() { return mediaUrl; }
    public void setMediaUrl(String mediaUrl) { this.mediaUrl = mediaUrl; }
}
