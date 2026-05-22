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
 * Rejects a promotion the user has already used up to {@code per_user_limit}.
 * An in-flight lock held by the user counts as one use.
 */
@Component
@Order(50)
@RequiredArgsConstructor
public class UserEligibilityValidator implements PromotionValidator {

    private final PromotionUsageRepository usageRepository;
    private final PromotionUsageLockRepository lockRepository;

    @Override
    public ValidationResult validate(Promotion promotion, CartContext context) {
        if (context.userId() == null) {
            return ValidationResult.pass();
        }
        int limit = promotion.getPerUserLimit() == null ? 1 : promotion.getPerUserLimit();
        long used = usageRepository.countByPromotionIdAndUserIdAndStatus(
                promotion.getId(), context.userId(), UsageStatus.APPLIED);
        boolean hasLock = lockRepository.existsByPromotionIdAndUserId(promotion.getId(), context.userId());
        if (used + (hasLock ? 1 : 0) >= limit) {
            return ValidationResult.failed("USER_LIMIT", "You have already used this promotion");
        }
        return ValidationResult.pass();
    }
}
