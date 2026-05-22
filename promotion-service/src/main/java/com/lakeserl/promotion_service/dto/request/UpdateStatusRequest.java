package com.lakeserl.promotion_service.dto.request;

import com.lakeserl.promotion_service.enums.PromotionStatus;

import jakarta.validation.constraints.NotNull;

/** Admin status transition (PAUSED / CANCELLED / ACTIVE). */
public record UpdateStatusRequest(
        @NotNull PromotionStatus status
) {
}
