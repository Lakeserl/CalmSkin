package com.lakeserl.promotion_service.service.engine.validator;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.lakeserl.promotion_service.entity.Promotion;
import com.lakeserl.promotion_service.service.engine.CartContext;

/**
 * Rejects a promotion when the cart total is below {@code min_order_value} or
 * the item count is below {@code min_item_quantity}. The quantity rule is only
 * checked when the request actually carries cart items.
 */
@Component
@Order(30)
public class MinOrderValueValidator implements PromotionValidator {

    @Override
    public ValidationResult validate(Promotion promotion, CartContext context) {
        if (promotion.getMinOrderValue() != null
                && context.safeCartTotal().compareTo(promotion.getMinOrderValue()) < 0) {
            return ValidationResult.failed("MIN_ORDER",
                    "Order total is below the minimum of " + promotion.getMinOrderValue());
        }
        if (context.hasCartItems()
                && promotion.getMinItemQuantity() != null
                && promotion.getMinItemQuantity() > 0
                && context.totalQuantity() < promotion.getMinItemQuantity()) {
            return ValidationResult.failed("MIN_QUANTITY",
                    "Requires at least " + promotion.getMinItemQuantity() + " items");
        }
        return ValidationResult.pass();
    }
}
