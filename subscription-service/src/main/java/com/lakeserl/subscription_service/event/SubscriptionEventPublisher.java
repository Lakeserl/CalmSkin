package com.lakeserl.subscription_service.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lakeserl.subscription_service.entity.OutboxEvent;
import com.lakeserl.subscription_service.entity.Subscription;
import com.lakeserl.subscription_service.enums.OutboxStatus;
import com.lakeserl.subscription_service.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Persists a {@code subscription.order.requested} event to the outbox table.
 * The {@link OutboxEventPublisher} scheduler then reliably forwards it to Kafka.
 *
 * <p>The order-service will need to consume this event and create a subscription order
 * using the provided subscriptionId, productId, addressId and userId as parameters.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SubscriptionEventPublisher {

    private static final String AGGREGATE_TYPE = "Subscription";
    private static final String EVENT_TYPE = "subscription.order.requested";

    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public void publishOrderRequested(Subscription subscription) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("subscriptionId", subscription.getId().toString());
        payload.put("userId", subscription.getUserId().toString());
        payload.put("productId", subscription.getProductId());
        payload.put("addressId", subscription.getAddressId().toString());
        payload.put("frequencyDays", subscription.getFrequencyDays());
        if (subscription.getNextOrderDueAt() != null) {
            payload.put("dueAt", subscription.getNextOrderDueAt()
                    .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        }

        try {
            String body = objectMapper.writeValueAsString(payload);
            OutboxEvent event = OutboxEvent.builder()
                    .aggregateType(AGGREGATE_TYPE)
                    .aggregateId(subscription.getId().toString())
                    .eventType(EVENT_TYPE)
                    .payload(body)
                    .status(OutboxStatus.PENDING)
                    .retryCount(0)
                    .build();
            outboxRepository.save(event);
            log.info("Outbox: subscription.order.requested saved for subscriptionId={}",
                    subscription.getId());
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException(
                    "Failed to serialize subscription.order.requested payload", ex);
        }
    }
}
