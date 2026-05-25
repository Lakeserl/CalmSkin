package com.lakeserl.shipping_service.service;

import com.lakeserl.shipping_service.entity.Shipment;
import com.lakeserl.shipping_service.enums.ShipmentStatus;
import com.lakeserl.shipping_service.enums.ShippingProvider;
import com.lakeserl.shipping_service.enums.TrackingEventSource;
import com.lakeserl.shipping_service.event.payload.inbound.OrderConfirmedEvent;
import com.lakeserl.shipping_service.event.producer.ShipmentEventPublisher;
import com.lakeserl.shipping_service.exception.InvalidShipmentStateException;
import com.lakeserl.shipping_service.exception.ShipmentNotFoundException;
import com.lakeserl.shipping_service.provider.MockShippingProviderClient;
import com.lakeserl.shipping_service.provider.ShippingProviderRegistry;
import com.lakeserl.shipping_service.repository.ShipmentRepository;
import com.lakeserl.shipping_service.repository.ShipmentTrackingEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Why this class matters: every status transition publishes shipping.updated
// downstream. If the service silently accepts an illegal transition or
// double-creates on a duplicate event, order-service and notification-service
// will desync. The tests below pin both the positive paths and the failure modes.
class ShipmentServiceImplTest {

    private ShipmentRepository shipmentRepo;
    private ShipmentTrackingEventRepository trackingRepo;
    private ShipmentMapper mapper;
    private ShippingProviderRegistry registry;
    private ShipmentEventPublisher publisher;
    private MockShippingProviderClient mockProvider;
    private ShipmentServiceImpl service;

    @BeforeEach
    void setUp() {
        shipmentRepo = mock(ShipmentRepository.class);
        trackingRepo = mock(ShipmentTrackingEventRepository.class);
        mapper = mock(ShipmentMapper.class);
        registry = mock(ShippingProviderRegistry.class);
        publisher = mock(ShipmentEventPublisher.class);
        mockProvider = new MockShippingProviderClient();
        ReflectionTestUtils.setField(mockProvider, "pickupEtaHours", 24L);
        ReflectionTestUtils.setField(mockProvider, "transitEtaHours", 72L);
        when(registry.get(ShippingProvider.MOCK)).thenReturn(mockProvider);

        service = new ShipmentServiceImpl(shipmentRepo, trackingRepo, mapper, registry, publisher);
        ReflectionTestUtils.setField(service, "defaultProvider", ShippingProvider.MOCK);
    }

    @Test
    void createFromOrder_createsShipmentAndPublishesEvent() {
        OrderConfirmedEvent event = sampleEvent();
        when(shipmentRepo.findByOrderId(99L)).thenReturn(Optional.empty());
        when(shipmentRepo.save(any(Shipment.class))).thenAnswer(inv -> {
            Shipment s = inv.getArgument(0);
            s.setId(1L);
            return s;
        });

        Shipment created = service.createFromOrder(event);

        assertThat(created.getProvider()).isEqualTo(ShippingProvider.MOCK);
        assertThat(created.getStatus()).isEqualTo(ShipmentStatus.PENDING);
        assertThat(created.getTrackingNumber()).startsWith("MK");
        assertThat(created.getRecipientName()).isEqualTo("Linh");
        verify(publisher, times(1)).publishShippingUpdated(any(Shipment.class));
    }

    @Test
    void createFromOrder_returnsExistingWhenDuplicate_doesNotCallProvider() {
        // Why: Kafka delivery is at-least-once. Re-processing order.confirmed must
        // not create a second shipment or re-publish shipping.updated, which would
        // double-notify the user.
        OrderConfirmedEvent event = sampleEvent();
        Shipment existing = Shipment.builder()
                .id(1L).orderId(99L).orderNumber("CS-1")
                .userId(UUID.randomUUID()).provider(ShippingProvider.MOCK)
                .status(ShipmentStatus.PENDING)
                .recipientName("Linh").recipientPhone("0900000000")
                .addressStreet("s").addressWard("w").addressDistrict("d").addressProvince("p")
                .addressCountry("VN").shippingFee(BigDecimal.ZERO).codAmount(BigDecimal.ZERO)
                .build();
        when(shipmentRepo.findByOrderId(99L)).thenReturn(Optional.of(existing));

        Shipment result = service.createFromOrder(event);

        assertThat(result).isSameAs(existing);
        verify(shipmentRepo, never()).save(any());
        verify(publisher, never()).publishShippingUpdated(any());
    }

    @Test
    void cancelByOrderId_rejectsAlreadyDeliveredShipment() {
        // Why: once delivered we cannot recall the parcel; surfacing the error
        // protects downstream from a refund being initiated against a completed order.
        Shipment delivered = Shipment.builder()
                .id(1L).orderId(99L).orderNumber("CS-1")
                .userId(UUID.randomUUID()).provider(ShippingProvider.MOCK)
                .status(ShipmentStatus.DELIVERED)
                .recipientName("Linh").recipientPhone("0900000000")
                .addressStreet("s").addressWard("w").addressDistrict("d").addressProvince("p")
                .addressCountry("VN").shippingFee(BigDecimal.ZERO).codAmount(BigDecimal.ZERO)
                .deliveredAt(LocalDateTime.now())
                .build();
        when(shipmentRepo.findByOrderId(99L)).thenReturn(Optional.of(delivered));

        assertThatThrownBy(() -> service.cancelByOrderId(99L, "customer requested"))
                .isInstanceOf(InvalidShipmentStateException.class)
                .hasMessageContaining("DELIVERED");

        verify(publisher, never()).publishShippingUpdated(any());
    }

    @Test
    void cancelByOrderId_missingShipment_throwsNotFound() {
        when(shipmentRepo.findByOrderId(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.cancelByOrderId(404L, "n/a"))
                .isInstanceOf(ShipmentNotFoundException.class);
    }

    @Test
    void updateStatus_invalidTransitionThrows() {
        // PENDING -> IN_TRANSIT skips PICKING and PICKED_UP; the carrier API
        // would never produce that jump and accepting it would corrupt tracking.
        Shipment shipment = Shipment.builder()
                .id(1L).orderId(99L).orderNumber("CS-1")
                .userId(UUID.randomUUID()).provider(ShippingProvider.MOCK)
                .status(ShipmentStatus.PENDING)
                .recipientName("Linh").recipientPhone("0900000000")
                .addressStreet("s").addressWard("w").addressDistrict("d").addressProvince("p")
                .addressCountry("VN").shippingFee(BigDecimal.ZERO).codAmount(BigDecimal.ZERO)
                .build();
        when(shipmentRepo.findById(1L)).thenReturn(Optional.of(shipment));

        assertThatThrownBy(() -> service.updateStatus(1L, ShipmentStatus.IN_TRANSIT,
                "skip ahead", null, TrackingEventSource.ADMIN_MANUAL, null))
                .isInstanceOf(InvalidShipmentStateException.class);
    }

    @Test
    void updateStatus_idempotentSameStatus_doesNotRepublish() {
        // Re-delivered webhook for the same status must not flood downstream.
        Shipment shipment = Shipment.builder()
                .id(1L).orderId(99L).orderNumber("CS-1")
                .userId(UUID.randomUUID()).provider(ShippingProvider.MOCK)
                .status(ShipmentStatus.IN_TRANSIT)
                .recipientName("Linh").recipientPhone("0900000000")
                .addressStreet("s").addressWard("w").addressDistrict("d").addressProvince("p")
                .addressCountry("VN").shippingFee(BigDecimal.ZERO).codAmount(BigDecimal.ZERO)
                .build();
        when(shipmentRepo.findById(1L)).thenReturn(Optional.of(shipment));

        Shipment result = service.updateStatus(1L, ShipmentStatus.IN_TRANSIT,
                "duplicate", null, TrackingEventSource.PROVIDER_WEBHOOK, null);

        assertThat(result.getStatus()).isEqualTo(ShipmentStatus.IN_TRANSIT);
        verify(publisher, never()).publishShippingUpdated(any());
    }

    private OrderConfirmedEvent sampleEvent() {
        return new OrderConfirmedEvent(
                "99",
                "CS-1",
                UUID.randomUUID(),
                new BigDecimal("350000"),
                "VNPAY",
                new BigDecimal("30000"),
                new OrderConfirmedEvent.ShippingAddress(
                        "Linh", "0900000000",
                        "12 Le Loi", "Ben Nghe", "1", "HCM", "VN"),
                List.of(new OrderConfirmedEvent.Item(
                        1L, 100L, null, "Toner", "SKU-1", 2,
                        new BigDecimal("150000"), new BigDecimal("300000")))
        );
    }
}
