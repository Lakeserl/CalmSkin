package com.lakeserl.promotion_service.service.engine.calculator;

import java.math.BigDecimal;

/** The discount a calculator produced: an order-amount cut and/or a shipping cut. */
public record DiscountBreakdown(
        BigDecimal totalDiscount,
        BigDecimal shippingDiscount
) {

    public static DiscountBreakdown amount(BigDecimal totalDiscount) {
        return new DiscountBreakdown(totalDiscount, BigDecimal.ZERO);
    }

    public static DiscountBreakdown shipping(BigDecimal shippingDiscount) {
        return new DiscountBreakdown(BigDecimal.ZERO, shippingDiscount);
    }
}
