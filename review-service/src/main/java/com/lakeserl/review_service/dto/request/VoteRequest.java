package com.lakeserl.review_service.dto.request;

import jakarta.validation.constraints.NotNull;

public record VoteRequest(
        @NotNull Boolean helpful
) {}
