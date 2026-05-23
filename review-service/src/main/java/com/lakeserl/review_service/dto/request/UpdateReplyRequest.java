package com.lakeserl.review_service.dto.request;

import jakarta.validation.constraints.NotBlank;

public record UpdateReplyRequest(
        @NotBlank String body
) {}
