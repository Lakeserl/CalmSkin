package com.lakeserl.shipping_service.event.producer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lakeserl.shipping_service.entity.OutboxEvent;
import com.lakeserl.shipping_service.entity.Shipment;
import com.lakeserl.shipping_service.enums.ShipmentStatus;
import com.lakeserl.shipping_service.enums.ShippingProvider;
import com.lakeserl.shipping_service.repository.OutboxRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Why this matters: the payload shape here is the contract order-service's
// ShippingEventConsumer deserializes. Required fields: orderId, shippingStatus,
// trackingNumber, shippingProvider, estimatedDelivery. Drift here breaks order
// status transitions downstream silently.
class ShipmentEventPublisherTest {

    @Test
    void publishShippingUpdated_writesOutboxWithRequiredFields() throws Exception {
        OutboxRepository repo = mock(OutboxRepository.class);
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
        ShipmentEventPublisher publisher = new ShipmentEventPublisher(repo, mapper);

        Shipment shipment = Shipment.builder()
                .id(1L).orderId(99L).orderNumber("CS-1")
                .userId(UUID.randomUUID()).provider(ShippingProvider.MOCK)
                .trackingNumber("MK12345").status(ShipmentStatus.IN_TRANSIT)
                .recipientName("Linh").recipientPhone("0900000000")
                .addressStreet("s").addressWard("w").addressDistrict("d").addressProvince("p")
                .addressCountry("VN").shippingFee(BigDecimal.ZERO).codAmount(BigDecimal.ZERO)
                .estimatedDeliveryAt(LocalDateTime.of(2026, 6, 1, 10, 0))
                .build();

        publisher.publishShippingUpdated(shipment);

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(repo).save(captor.capture());
        OutboxEvent saved = captor.getValue();

        assertThat(saved.getEventType()).isEqualTo("shipping.updated");
        assertThat(saved.getAggregateId()).isEqualTo("99");
        assertThat(saved.getPayload())
                .contains("\"orderId\":\"99\"")
                .contains("\"trackingNumber\":\"MK12345\"")
                .contains("\"shippingStatus\":\"SHIPPED\"")
                .contains("\"shippingProvider\":\"MOCK\"");
    }

    @Test
    void publishShippingUpdated_pendingMapsToPending_notShippedOrDelivered() {
        // Why: only PICKED_UP / IN_TRANSIT / OUT_FOR_DELIVERY collapse to SHIPPED.
        // A PENDING shipment must not advance order-service's status prematurely.
        OutboxRepository repo = mock(OutboxRepository.class);
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
        ShipmentEventPublisher publisher = new ShipmentEventPublisher(repo, mapper);

        Shipment pending = Shipment.builder()
                .id(2L).orderId(100L).orderNumber("CS-2")
                .userId(UUID.randomUUID()).provider(ShippingProvider.MOCK)
                .trackingNumber("MK99999").status(ShipmentStatus.PENDING)
                .recipientName("A").recipientPhone("0900000001")
                .addressStreet("s").addressWard("w").addressDistrict("d").addressProvince("p")
                .addressCountry("VN").shippingFee(BigDecimal.ZERO).codAmount(BigDecimal.ZERO)
                .build();

        publisher.publishShippingUpdated(pending);

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(repo).save(captor.capture());
        assertThat(captor.getValue().getPayload())
                .contains("\"shippingStatus\":\"PENDING\"")
                .doesNotContain("\"shippingStatus\":\"SHIPPED\"");
    }

    private static <T> T any() { return org.mockito.ArgumentMatchers.any(); }
}
