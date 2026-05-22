package com.lakeserl.promotion_service.service.engine;

/**
 * The promotion discount engine. Given a cart, it resolves every applicable
 * promotion, applies stack rules, and returns the combined discount plus
 * flash-sale prices.
 */
public interface PromotionEngine {

    DiscountResult calculate(CartContext context);
}
