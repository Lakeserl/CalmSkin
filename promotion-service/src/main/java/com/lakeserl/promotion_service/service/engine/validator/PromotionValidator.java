package com.lakeserl.promotion_service.service.engine.validator;

import com.lakeserl.promotion_service.entity.Promotion;
import com.lakeserl.promotion_service.service.engine.CartContext;

/**
 * One link in the promotion validation chain. Every validator must pass for a
 * promotion to be eligible. Implementations are Spring beans collected into a
 * list by the evaluator.
 */
public interface PromotionValidator {

    ValidationResult validate(Promotion promotion, CartContext context);
}
