package com.lakeserl.promotion_service.dto.request;

import java.util.List;
import java.util.UUID;

/**
 * Admin request to bulk-generate campaign voucher codes for a promotion.
 * Provide {@code assignedUserIds} to mint one user-bound code each, or
 * {@code count} to mint that many unbound (public-pool) codes.
 */
public record GenerateVoucherCodesRequest(
        Integer count,
        List<UUID> assignedUserIds
) {
}
