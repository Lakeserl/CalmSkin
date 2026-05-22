package com.lakeserl.promotion_service.dto.request;

import jakarta.validation.constraints.NotBlank;

/** Body for the confirm / release internal endpoints. */
public record OrderRefRequest(
        @NotBlank String orderId
) {
}
