package com.lakeserl.promotion_service.dto.request;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

/** Admin bulk voucher assignment. */
public record AssignBulkRequest(
        @NotNull Long promotionId,
        @NotEmpty List<UUID> userIds,
        String source
) {
}
