package com.lakeserl.shipping_service.provider;

import com.lakeserl.shipping_service.enums.ShippingProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

// Local-only carrier used for Phase 3 development before real GHN/GHTK
// adapters land. Generates deterministic-looking tracking numbers and ETAs
// from configured offsets. Cancellation always succeeds.
@Slf4j
@Component
public class MockShippingProviderClient implements ShippingProviderClient {

    @Value("${app.shipping.pickup-eta-hours:24}")
    private long pickupEtaHours;

    @Value("${app.shipping.transit-eta-hours:72}")
    private long transitEtaHours;

    @Override
    public ShippingProvider provider() {
        return ShippingProvider.MOCK;
    }

    @Override
    public CreateShipmentResponse createShipment(CreateShipmentRequest request) {
        String providerOrderId = "MOCK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String trackingNumber = "MK" + System.currentTimeMillis();
        LocalDateTime now = LocalDateTime.now();
        log.info("[MOCK] createShipment orderNumber={} -> providerOrderId={} tracking={}",
                request.orderNumber(), providerOrderId, trackingNumber);
        return CreateShipmentResponse.builder()
                .providerOrderId(providerOrderId)
                .trackingNumber(trackingNumber)
                .estimatedPickupAt(now.plusHours(pickupEtaHours))
                .estimatedDeliveryAt(now.plusHours(pickupEtaHours + transitEtaHours))
                .build();
    }

    @Override
    public void cancelShipment(String providerOrderId, String reason) {
        log.info("[MOCK] cancelShipment providerOrderId={} reason={}", providerOrderId, reason);
    }
}
