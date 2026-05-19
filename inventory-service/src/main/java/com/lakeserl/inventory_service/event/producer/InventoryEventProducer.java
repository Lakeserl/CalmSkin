package com.lakeserl.inventory_service.event.producer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lakeserl.inventory_service.entity.Inventory;
import com.lakeserl.inventory_service.entity.OutboxEvent;
import com.lakeserl.inventory_service.enums.OutboxStatus;
import com.lakeserl.inventory_service.event.payload.LowStockEvent;
import com.lakeserl.inventory_service.event.payload.OutOfStockEvent;
import com.lakeserl.inventory_service.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InventoryEventProducer {
    private static final String AGGREGATE_TYPE = "Inventory";

    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public void publish(String eventType, String aggregateId, Object payload) {
        String body = toJson(payload);
        OutboxEvent event = OutboxEvent.builder()
                .aggregateType(AGGREGATE_TYPE)
                .aggregateId(aggregateId)
                .eventType(eventType)
                .payload(body)
                .status(OutboxStatus.PENDING)
                .retryCount(0)
                .build();
        outboxRepository.save(event);
    }

    public void publishStockAlerts(Inventory inventory, int previousAvailable) {
        int currentAvailable = inventory.getQuantityAvailable();
        Integer thresholdValue = inventory.getLowStockThreshold();
        int threshold = thresholdValue == null ? 0 : thresholdValue;
        String aggregateId = buildAggregateId(inventory.getProductId(), inventory.getVariantId());

        if (previousAvailable >= threshold && currentAvailable < threshold) {
            publish("inventory.low-stock", aggregateId,
                    new LowStockEvent(inventory.getProductId(), inventory.getVariantId(), currentAvailable, threshold));
        }

        if (previousAvailable > 0 && currentAvailable == 0) {
            publish("inventory.out-of-stock", aggregateId,
                    new OutOfStockEvent(inventory.getProductId(), inventory.getVariantId(), currentAvailable));
        }

        if (previousAvailable == 0 && currentAvailable > 0) {
            publish("inventory.back-in-stock", aggregateId,
                    new OutOfStockEvent(inventory.getProductId(), inventory.getVariantId(), currentAvailable));
        }
    }

    private String buildAggregateId(Long productId, Long variantId) {
        if (variantId == null) {
            return productId + ":default";
        }
        return productId + ":" + variantId;
    }

    private String toJson(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize outbox payload", ex);
        }
    }
}
