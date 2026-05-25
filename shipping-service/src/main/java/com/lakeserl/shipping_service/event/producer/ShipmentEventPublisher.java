package com.lakeserl.shipping_service.event.producer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lakeserl.shipping_service.entity.OutboxEvent;
import com.lakeserl.shipping_service.entity.Shipment;
import com.lakeserl.shipping_service.enums.OutboxStatus;
import com.lakeserl.shipping_service.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

// Publishes shipping.updated through the outbox so order-service +
// notification-service receive a consistent contract regardless of whether
// the update came from a webhook, admin action, or scheduler.
@Slf4j
@Component
@RequiredArgsConstructor
public class ShipmentEventPublisher {

    private static final String AGGREGATE_TYPE = "Shipment";
    private static final String EVENT_TYPE = "shipping.updated";

    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public void publishShippingUpdated(Shipment shipment) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("orderId", shipment.getOrderId().toString());
        payload.put("orderNumber", shipment.getOrderNumber());
        payload.put("shippingProvider", shipment.getProvider().name());
        payload.put("trackingNumber", shipment.getTrackingNumber());
        // shippingStatus mapped to the vocabulary the order-service ShippingEventConsumer
        // already understands (SHIPPED / DELIVERED). Other states are passed through.
        payload.put("shippingStatus", toExternalStatus(shipment));
        if (shipment.getEstimatedDeliveryAt() != null) {
            payload.put("estimatedDelivery",
                    shipment.getEstimatedDeliveryAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        }
        if (shipment.getDeliveredAt() != null) {
            payload.put("actualDelivery",
                    shipment.getDeliveredAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        }

        try {
            String body = objectMapper.writeValueAsString(payload);
            OutboxEvent event = OutboxEvent.builder()
                    .aggregateType(AGGREGATE_TYPE)
                    .aggregateId(shipment.getOrderId().toString())
                    .eventType(EVENT_TYPE)
                    .payload(body)
                    .status(OutboxStatus.PENDING)
                    .retryCount(0)
                    .build();
            outboxRepository.save(event);
            log.info("Outbox: shipping.updated saved for orderId={} status={}",
                    shipment.getOrderId(), shipment.getStatus());
        } catch (JsonProcessingException ex) {
            // Rolling back the surrounding @Transactional is the safer failure
            // mode — losing the event silently would desync order status.
            throw new IllegalStateException("Failed to serialize shipping.updated payload", ex);
        }
    }

    private String toExternalStatus(Shipment shipment) {
        return switch (shipment.getStatus()) {
            case PICKED_UP, IN_TRANSIT, OUT_FOR_DELIVERY -> "SHIPPED";
            case DELIVERED -> "DELIVERED";
            default -> shipment.getStatus().name();
        };
    }
}
