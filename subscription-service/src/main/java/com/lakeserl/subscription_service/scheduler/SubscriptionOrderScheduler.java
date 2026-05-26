package com.lakeserl.subscription_service.scheduler;

import com.lakeserl.subscription_service.entity.Subscription;
import com.lakeserl.subscription_service.event.SubscriptionEventPublisher;
import com.lakeserl.subscription_service.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Daily cron job that finds all ACTIVE subscriptions whose {@code next_order_due_at}
 * is at or before now, publishes a {@code subscription.order.requested} Kafka event
 * for each one, then advances the schedule by {@code frequencyDays}.
 *
 * <p>The event is written to the outbox (not published directly) so Kafka delivery
 * is guaranteed even if the broker is temporarily unavailable.
 *
 * <p>Configured via {@code app.subscription.scheduler-cron} (default: 6 AM daily).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SubscriptionOrderScheduler {

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionEventPublisher eventPublisher;

    @Value("${app.subscription.scheduler-batch-size:200}")
    private int batchSize;

    @Scheduled(cron = "${app.subscription.scheduler-cron:0 0 6 * * *}")
    @Transactional
    public void processDueSubscriptions() {
        LocalDateTime now = LocalDateTime.now();
        log.info("SubscriptionOrderScheduler: starting run at {}", now);

        List<Subscription> due = subscriptionRepository.findDueSubscriptions(now);

        if (due.isEmpty()) {
            log.info("SubscriptionOrderScheduler: no due subscriptions");
            return;
        }

        // Safety guard: cap the batch to avoid processing too many at once.
        // Remaining items will be picked up on the next cron run (they are still
        // past-due so the WHERE clause will return them again).
        List<Subscription> batch = due.size() > batchSize ? due.subList(0, batchSize) : due;
        log.info("SubscriptionOrderScheduler: processing {} / {} due subscriptions",
                batch.size(), due.size());

        int processed = 0;
        int failed = 0;

        for (Subscription subscription : batch) {
            try {
                // Publish the event (writes to outbox, same transaction)
                eventPublisher.publishOrderRequested(subscription);

                // Advance the schedule
                LocalDateTime nextDue = now.plusDays(subscription.getFrequencyDays());
                subscription.setLastOrderedAt(now);
                subscription.setNextOrderDueAt(nextDue);
                subscriptionRepository.save(subscription);

                log.info("Scheduled order for subscriptionId={} nextDue={}",
                        subscription.getId(), nextDue);
                processed++;
            } catch (Exception ex) {
                // Log and continue — one failing subscription must not abort the whole batch.
                // The subscription retains its original nextOrderDueAt and will be
                // retried on the next cron execution.
                log.error("Failed to process subscriptionId={}: {}",
                        subscription.getId(), ex.getMessage(), ex);
                failed++;
            }
        }

        log.info("SubscriptionOrderScheduler: done — processed={} failed={}", processed, failed);
    }
}
