package com.lakeserl.promotion_service.service.engine.validator;

import java.time.LocalDateTime;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.lakeserl.promotion_service.entity.Promotion;
import com.lakeserl.promotion_service.service.engine.CartContext;

/** Rejects a promotion whose active window has not started or has ended. */
@Component
@Order(10)
public class TimeRangeValidator implements PromotionValidator {

    @Override
    public ValidationResult validate(Promotion promotion, CartContext context) {
        LocalDateTime now = LocalDateTime.now();
        if (promotion.getStartsAt() != null && now.isBefore(promotion.getStartsAt())) {
            return ValidationResult.failed("NOT_STARTED", "Promotion has not started yet");
        }
        if (promotion.getEndsAt() != null && now.isAfter(promotion.getEndsAt())) {
            return ValidationResult.failed("EXPIRED", "Promotion has ended");
        }
        return ValidationResult.pass();
    }
}
