package com.lakeserl.promotion_service.service.engine;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.lakeserl.promotion_service.entity.Promotion;
import com.lakeserl.promotion_service.entity.PromotionTier;
import com.lakeserl.promotion_service.enums.DiscountType;
import com.lakeserl.promotion_service.repository.PromotionTierRepository;
import com.lakeserl.promotion_service.service.engine.calculator.DiscountBreakdown;
import com.lakeserl.promotion_service.service.engine.calculator.DiscountCalculator;
import com.lakeserl.promotion_service.service.engine.validator.PromotionValidator;
import com.lakeserl.promotion_service.service.engine.validator.ValidationResult;

import lombok.extern.slf4j.Slf4j;

/**
 * Runs one promotion through the full validator chain and, if it passes,
 * through the matching discount calculator. A promotion with tier rows is
 * resolved to the best tier the cart qualifies for; otherwise the promotion's
 * own discount type/value is used.
 */
@Slf4j
@Component
public class PromotionEvaluator {

    private final List<PromotionValidator> validators;
    private final Map<DiscountType, DiscountCalculator> calculators = new EnumMap<>(DiscountType.class);
    private final PromotionTierRepository tierRepository;

    public PromotionEvaluator(List<PromotionValidator> validators,
                              List<DiscountCalculator> calculatorList,
                              PromotionTierRepository tierRepository) {
        this.validators = validators;
        this.tierRepository = tierRepository;
        for (DiscountCalculator calculator : calculatorList) {
            calculators.put(calculator.type(), calculator);
        }
    }

    public EvaluatedPromotion evaluate(Promotion promotion, CartContext context) {
        for (PromotionValidator validator : validators) {
            ValidationResult result = validator.validate(promotion, context);
            if (!result.passed()) {
                return EvaluatedPromotion.rejected(promotion, result.failureMessage());
            }
        }

        DiscountType discountType = promotion.getDiscountType();
        BigDecimal discountValue = promotion.getDiscountValue();

        List<PromotionTier> tiers = tierRepository.findByPromotionIdOrderBySortOrderAsc(promotion.getId());
        if (!tiers.isEmpty()) {
            PromotionTier tier = pickBestTier(tiers, context);
            if (tier == null) {
                return EvaluatedPromotion.rejected(promotion, "No tier threshold met");
            }
            discountType = tier.getDiscountType();
            discountValue = tier.getDiscountValue();
        }

        DiscountCalculator calculator = calculators.get(discountType);
        if (calculator == null) {
            return EvaluatedPromotion.rejected(promotion,
                    "Discount type " + discountType + " is not supported");
        }
        DiscountBreakdown breakdown = calculator.calculate(
                context.safeCartTotal(), context.safeShippingFee(),
                discountValue, promotion.getMaxDiscountAmount());
        return EvaluatedPromotion.accepted(promotion, breakdown.totalDiscount(), breakdown.shippingDiscount());
    }

    /** Picks the qualifying tier that gives the customer the largest discount value. */
    private PromotionTier pickBestTier(List<PromotionTier> tiers, CartContext context) {
        PromotionTier best = null;
        for (PromotionTier tier : tiers) {
            boolean quantityOk = tier.getMinQuantity() == null
                    || context.totalQuantity() >= tier.getMinQuantity();
            boolean valueOk = tier.getMinValue() == null
                    || context.safeCartTotal().compareTo(tier.getMinValue()) >= 0;
            if (quantityOk && valueOk
                    && (best == null || tier.getDiscountValue().compareTo(best.getDiscountValue()) > 0)) {
                best = tier;
            }
        }
        return best;
    }
}
