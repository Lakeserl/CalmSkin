package com.lakeserl.promotion_service.service.engine.validator;

import java.util.Collections;
import java.util.Set;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.lakeserl.promotion_service.entity.Promotion;
import com.lakeserl.promotion_service.service.engine.CartContext;
import com.lakeserl.promotion_service.support.CsvIds;

/**
 * Checks the product scope of a promotion against the cart. Category and brand
 * scope cannot be evaluated here (the service holds no product catalogue), so
 * only product-id scope is enforced; a cart-less request (e.g. plain voucher
 * validation) skips the check.
 */
@Component
@Order(40)
public class ProductScopeValidator implements PromotionValidator {

    @Override
    public ValidationResult validate(Promotion promotion, CartContext context) {
        if (!context.hasCartItems()) {
            return ValidationResult.pass();
        }
        Set<Long> cartProducts = context.productIds();
        Set<Long> applicable = CsvIds.parse(promotion.getApplicableProductIds());
        Set<Long> excluded = CsvIds.parse(promotion.getExcludedProductIds());

        if (!applicable.isEmpty() && Collections.disjoint(applicable, cartProducts)) {
            return ValidationResult.failed("SCOPE", "No applicable products in the cart");
        }
        if (!excluded.isEmpty() && !cartProducts.isEmpty() && excluded.containsAll(cartProducts)) {
            return ValidationResult.failed("SCOPE", "All cart products are excluded from this promotion");
        }
        return ValidationResult.pass();
    }
}
