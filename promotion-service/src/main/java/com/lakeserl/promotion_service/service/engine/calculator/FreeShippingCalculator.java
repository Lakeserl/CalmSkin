package com.lakeserl.promotion_service.service.engine.calculator;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import com.lakeserl.promotion_service.enums.DiscountType;

/** Free-shipping discount: waives the whole shipping fee, no order-amount cut. */
@Component
public class FreeShippingCalculator implements DiscountCalculator {

    @Override
    public DiscountType type() {
        return DiscountType.FREE_SHIPPING;
    }

    @Override
    public DiscountBreakdown calculate(BigDecimal baseAmount, BigDecimal shippingFee,
                                       BigDecimal discountValue, BigDecimal maxDiscount) {
        return DiscountBreakdown.shipping(shippingFee == null ? BigDecimal.ZERO : shippingFee);
    }
}
