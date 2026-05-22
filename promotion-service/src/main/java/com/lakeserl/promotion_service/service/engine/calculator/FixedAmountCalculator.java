package com.lakeserl.promotion_service.service.engine.calculator;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import com.lakeserl.promotion_service.enums.DiscountType;

/** Fixed-amount discount, capped at the base amount so a total never goes negative. */
@Component
public class FixedAmountCalculator implements DiscountCalculator {

    @Override
    public DiscountType type() {
        return DiscountType.FIXED_AMOUNT;
    }

    @Override
    public DiscountBreakdown calculate(BigDecimal baseAmount, BigDecimal shippingFee,
                                       BigDecimal discountValue, BigDecimal maxDiscount) {
        BigDecimal discount = discountValue.min(baseAmount);
        return DiscountBreakdown.amount(discount);
    }
}
