package com.lakeserl.promotion_service.scheduler;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.lakeserl.promotion_service.entity.PromotionUsageLock;
import com.lakeserl.promotion_service.repository.PromotionUsageLockRepository;
import com.lakeserl.promotion_service.service.PromotionService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Reclaims promotion usage locks whose TTL has elapsed (the order was never
 * confirmed). Runs the full release flow per order so held flash-sale slots
 * are returned to the available pool, not just the lock rows deleted.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UsageLockCleanup {

    private final PromotionUsageLockRepository lockRepository;
    private final PromotionService promotionService;

    @Scheduled(fixedDelay = 60_000)
    public void releaseExpiredLocks() {
        Set<String> orderIds = lockRepository.findByExpiresAtBefore(LocalDateTime.now()).stream()
                .map(PromotionUsageLock::getOrderId)
                .collect(Collectors.toSet());
        int released = 0;
        for (String orderId : orderIds) {
            try {
                promotionService.release(orderId);
                released++;
            } catch (Exception ex) {
                log.error("Failed to release expired promotion lock for order {}", orderId, ex);
            }
        }
        if (released > 0) {
            log.info("Released expired promotion locks for {} orders", released);
        }
    }
}
