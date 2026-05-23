package com.lakeserl.review_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record PresignRequest(
        @NotBlank String filename,
        @NotBlank @Pattern(regexp = "image/(jpeg|png|webp|gif)|video/(mp4|mov)") String contentType
) {}
