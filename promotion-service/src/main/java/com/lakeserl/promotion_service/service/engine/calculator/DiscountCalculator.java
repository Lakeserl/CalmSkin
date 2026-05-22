package com.lakeserl.promotion_service.service.engine.calculator;

import java.math.BigDecimal;

import com.lakeserl.promotion_service.enums.DiscountType;

/**
 * Strategy that turns a discount value into an actual money amount. Pure
 * function of its inputs - no entity or repository dependency - so it is reused
 * for both flat promotions and per-tier discounts.
 */
public interface DiscountCalculator {

    DiscountType type();

    DiscountBreakdown calculate(BigDecimal baseAmount,
                                BigDecimal shippingFee,
                                BigDecimal discountValue,
                                BigDecimal maxDiscount);
}
