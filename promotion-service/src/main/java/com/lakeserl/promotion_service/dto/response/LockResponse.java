package com.lakeserl.promotion_service.dto.response;

import java.math.BigDecimal;
import java.util.List;

/** Result of a promotion lock attempt. */
public record LockResponse(
        boolean locked,
        List<Long> lockedPromotions,
        BigDecimal totalDiscount
) {
}
