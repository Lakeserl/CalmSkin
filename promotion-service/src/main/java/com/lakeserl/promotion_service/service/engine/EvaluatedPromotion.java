package com.lakeserl.promotion_service.service.engine;

import java.math.BigDecimal;

import com.lakeserl.promotion_service.entity.Promotion;

/**
 * Outcome of running one promotion through the validator chain and calculator.
 * When {@code eligible} is false, {@code reason} explains why and the discount
 * amounts are zero.
 */
public record EvaluatedPromotion(
        Promotion promotion,
        boolean eligible,
        String reason,
        BigDecimal discount,
        BigDecimal shippingDiscount
) {

    public static EvaluatedPromotion rejected(Promotion promotion, String reason) {
        return new EvaluatedPromotion(promotion, false, reason, BigDecimal.ZERO, BigDecimal.ZERO);
    }

    public static EvaluatedPromotion accepted(Promotion promotion, BigDecimal discount, BigDecimal shippingDiscount) {
        return new EvaluatedPromotion(promotion, true, null, discount, shippingDiscount);
    }

    /** Total monetary benefit, used to rank competing non-stackable promotions. */
    public BigDecimal totalBenefit() {
        return discount.add(shippingDiscount);
    }
}
