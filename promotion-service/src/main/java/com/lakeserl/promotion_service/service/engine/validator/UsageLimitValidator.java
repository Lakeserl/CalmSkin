package com.lakeserl.promotion_service.service.engine.validator;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.lakeserl.promotion_service.entity.Promotion;
import com.lakeserl.promotion_service.enums.UsageStatus;
import com.lakeserl.promotion_service.repository.PromotionUsageLockRepository;
import com.lakeserl.promotion_service.repository.PromotionUsageRepository;
import com.lakeserl.promotion_service.service.engine.CartContext;

import lombok.RequiredArgsConstructor;

/**
 * Rejects a promotion whose total usage limit is reached. Counts confirmed
 * usages plus in-flight locks so concurrent checkouts cannot oversell.
 */
@Component
@Order(20)
@RequiredArgsConstructor
public class UsageLimitValidator implements PromotionValidator {

    private final PromotionUsageRepository usageRepository;
    private final PromotionUsageLockRepository lockRepository;

    @Override
    public ValidationResult validate(Promotion promotion, CartContext context) {
        if (promotion.getTotalUsageLimit() == null) {
            return ValidationResult.pass();
        }
        long used = usageRepository.countByPromotionIdAndStatus(promotion.getId(), UsageStatus.APPLIED);
        long locked = lockRepository.countByPromotionId(promotion.getId());
        if (used + locked >= promotion.getTotalUsageLimit()) {
            return ValidationResult.failed("USAGE_LIMIT", "Promotion usage limit reached");
        }
        return ValidationResult.pass();
    }
}
