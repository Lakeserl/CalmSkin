package com.lakeserl.promotion_service.dto.response;

import java.util.UUID;

/** One generated campaign voucher code. */
public record VoucherCodeResponse(
        String code,
        UUID assignedUserId,
        String status
) {
}
