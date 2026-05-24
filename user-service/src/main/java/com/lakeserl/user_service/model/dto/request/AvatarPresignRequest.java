package com.lakeserl.user_service.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

/**
 * Request body for avatar pre-signed URL generation.
 * filename: the original client filename (used only for extension extraction; never stored as-is).
 * contentType: declared MIME type. The service validates via magic-byte detection, not this field.
 * sizeBytes: declared file size; server rejects if over limit (5 MB).
 */
public class AvatarPresignRequest {

    @NotBlank
    private String filename;

    @NotBlank
    @Pattern(regexp = "image/(jpeg|png|webp)", message = "Only image/jpeg, image/png, image/webp are allowed")
    private String contentType;

    @Positive
    private long sizeBytes;

    public AvatarPresignRequest() {}

    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public long getSizeBytes() { return sizeBytes; }
    public void setSizeBytes(long sizeBytes) { this.sizeBytes = sizeBytes; }
}
