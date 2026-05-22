package com.lakeserl.promotion_service.scheduler;

import java.time.LocalDateTime;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.lakeserl.promotion_service.entity.Promotion;
import com.lakeserl.promotion_service.enums.PromotionStatus;
import com.lakeserl.promotion_service.repository.PromotionRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Auto-transitions promotion status every minute: SCHEDULED promotions whose
 * start time has arrived go ACTIVE, and ACTIVE promotions past their end time
 * go EXPIRED.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PromotionStatusScheduler {

    private final PromotionRepository promotionRepository;

    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void syncStatuses() {
        LocalDateTime now = LocalDateTime.now();
        int activated = 0;
        int expired = 0;

        for (Promotion promotion : promotionRepository
                .findByStatusAndStartsAtLessThanEqual(PromotionStatus.SCHEDULED, now)) {
            if (promotion.getEndsAt().isAfter(now)) {
                promotion.setStatus(PromotionStatus.ACTIVE);
                activated++;
            } else {
                promotion.setStatus(PromotionStatus.EXPIRED);
                expired++;
            }
            promotionRepository.save(promotion);
        }

        for (Promotion promotion : promotionRepository
                .findByStatusAndEndsAtLessThan(PromotionStatus.ACTIVE, now)) {
            promotion.setStatus(PromotionStatus.EXPIRED);
            promotionRepository.save(promotion);
            expired++;
        }

        if (activated + expired > 0) {
            log.info("Promotion status sync: {} activated, {} expired", activated, expired);
        }
    }
}
