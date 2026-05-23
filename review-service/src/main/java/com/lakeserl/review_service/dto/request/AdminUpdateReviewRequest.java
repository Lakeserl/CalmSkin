package com.lakeserl.review_service.dto.request;

import com.lakeserl.review_service.enums.ReviewStatus;
import jakarta.validation.constraints.NotNull;

public record AdminUpdateReviewRequest(
        @NotNull ReviewStatus status,
        String adminNote
) {}
