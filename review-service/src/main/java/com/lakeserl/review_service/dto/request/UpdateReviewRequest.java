package com.lakeserl.review_service.dto.request;

import jakarta.validation.constraints.*;

import java.util.List;

public record UpdateReviewRequest(
        @Min(1) @Max(5) Short rating,
        @Size(max = 255) String title,
        @Size(max = 5000) String body,
        @Pattern(regexp = "OILY|DRY|COMBINATION|SENSITIVE|NORMAL") String skinType,
        @Pattern(regexp = "18-24|25-34|35-44|45\\+") String ageRange,
        @Min(1) @Max(5) Short skinEffectRating,
        @Min(1) @Max(5) Short textureRating,
        @Min(1) @Max(5) Short scentRating,
        @Min(1) @Max(5) Short packagingRating,
        @Min(1) @Max(5) Short valueRating,
        @Size(max = 5) List<@NotBlank String> mediaUrls
) {}
