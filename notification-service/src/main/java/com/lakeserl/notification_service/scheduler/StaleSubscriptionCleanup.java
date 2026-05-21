package com.lakeserl.notification_service.scheduler;

import java.time.LocalDateTime;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.lakeserl.notification_service.repository.WebPushSubscriptionRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Weekly cleanup of dead Web Push subscriptions: those deactivated after a
 * 404/410 from the browser relay, or unused for 90 days.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StaleSubscriptionCleanup {

    private static final int STALE_DAYS = 90;

    private final WebPushSubscriptionRepository subscriptionRepository;

    @Scheduled(cron = "0 0 3 * * SUN", zone = "Asia/Ho_Chi_Minh")
    @Transactional
    public void cleanup() {
        int removed = subscriptionRepository.deleteStale(LocalDateTime.now().minusDays(STALE_DAYS));
        if (removed > 0) {
            log.info("Removed {} stale web push subscriptions", removed);
        }
    }
}
