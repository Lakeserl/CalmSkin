package com.lakeserl.promotion_service.service.engine.calculator;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.stereotype.Component;

import com.lakeserl.promotion_service.enums.DiscountType;

/**
 * Percentage discount. {@code discountValue} is a plain percentage (20 = 20%);
 * the result is capped at {@code maxDiscount} (if set) and at the base amount.
 */
@Component
public class PercentageCalculator implements DiscountCalculator {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    @Override
    public DiscountType type() {
        return DiscountType.PERCENTAGE;
    }

    @Override
    public DiscountBreakdown calculate(BigDecimal baseAmount, BigDecimal shippingFee,
                                       BigDecimal discountValue, BigDecimal maxDiscount) {
        BigDecimal discount = baseAmount
                .multiply(discountValue)
                .divide(HUNDRED, 2, RoundingMode.HALF_UP);
        if (maxDiscount != null && discount.compareTo(maxDiscount) > 0) {
            discount = maxDiscount;
        }
        if (discount.compareTo(baseAmount) > 0) {
            discount = baseAmount;
        }
        return DiscountBreakdown.amount(discount);
    }
}
