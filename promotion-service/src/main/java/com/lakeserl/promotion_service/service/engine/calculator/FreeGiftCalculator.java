package com.lakeserl.promotion_service.service.engine.calculator;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import com.lakeserl.promotion_service.enums.DiscountType;

/**
 * Free-gift "discount": a gift-with-purchase adds a free product rather than
 * cutting the order total, so the monetary discount is always zero. The gift
 * products themselves are resolved separately by the engine
 * ({@code computeFreeGifts}). This calculator exists so the evaluator's
 * DiscountType lookup does not miss for FREE_GIFT promotions.
 */
@Component
public class FreeGiftCalculator implements DiscountCalculator {

    @Override
    public DiscountType type() {
        return DiscountType.FREE_GIFT;
    }

    @Override
    public DiscountBreakdown calculate(BigDecimal baseAmount, BigDecimal shippingFee,
                                       BigDecimal discountValue, BigDecimal maxDiscount) {
        return DiscountBreakdown.amount(BigDecimal.ZERO);
    }
}
