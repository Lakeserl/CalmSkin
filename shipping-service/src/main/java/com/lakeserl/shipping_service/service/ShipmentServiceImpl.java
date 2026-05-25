package com.lakeserl.shipping_service.service;

import com.lakeserl.shipping_service.dto.response.ShipmentDTO;
import com.lakeserl.shipping_service.entity.Shipment;
import com.lakeserl.shipping_service.entity.ShipmentTrackingEvent;
import com.lakeserl.shipping_service.enums.ShipmentStatus;
import com.lakeserl.shipping_service.enums.ShippingProvider;
import com.lakeserl.shipping_service.enums.TrackingEventSource;
import com.lakeserl.shipping_service.event.payload.inbound.OrderConfirmedEvent;
import com.lakeserl.shipping_service.event.producer.ShipmentEventPublisher;
import com.lakeserl.shipping_service.exception.InvalidShipmentStateException;
import com.lakeserl.shipping_service.exception.ShipmentNotFoundException;
import com.lakeserl.shipping_service.provider.CreateShipmentRequest;
import com.lakeserl.shipping_service.provider.CreateShipmentResponse;
import com.lakeserl.shipping_service.provider.ShippingProviderRegistry;
import com.lakeserl.shipping_service.repository.ShipmentRepository;
import com.lakeserl.shipping_service.repository.ShipmentTrackingEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShipmentServiceImpl implements ShipmentService {

    // Allowed status transitions. Terminal states (DELIVERED, CANCELLED, RETURNED)
    // have no outgoing edges. Encoded once here rather than scattered through
    // call sites so the rules show up in a single audit.
    private static final Map<ShipmentStatus, Set<ShipmentStatus>> ALLOWED = new EnumMap<>(ShipmentStatus.class);
    static {
        ALLOWED.put(ShipmentStatus.PENDING, EnumSet.of(ShipmentStatus.PICKING, ShipmentStatus.CANCELLED, ShipmentStatus.FAILED));
        ALLOWED.put(ShipmentStatus.PICKING, EnumSet.of(ShipmentStatus.PICKED_UP, ShipmentStatus.CANCELLED, ShipmentStatus.FAILED));
        ALLOWED.put(ShipmentStatus.PICKED_UP, EnumSet.of(ShipmentStatus.IN_TRANSIT, ShipmentStatus.FAILED));
        ALLOWED.put(ShipmentStatus.IN_TRANSIT, EnumSet.of(ShipmentStatus.OUT_FOR_DELIVERY, ShipmentStatus.FAILED));
        ALLOWED.put(ShipmentStatus.OUT_FOR_DELIVERY, EnumSet.of(ShipmentStatus.DELIVERED, ShipmentStatus.FAILED, ShipmentStatus.RETURNED));
        ALLOWED.put(ShipmentStatus.FAILED, EnumSet.of(ShipmentStatus.RETURNED, ShipmentStatus.IN_TRANSIT));
        ALLOWED.put(ShipmentStatus.DELIVERED, EnumSet.noneOf(ShipmentStatus.class));
        ALLOWED.put(ShipmentStatus.CANCELLED, EnumSet.noneOf(ShipmentStatus.class));
        ALLOWED.put(ShipmentStatus.RETURNED, EnumSet.noneOf(ShipmentStatus.class));
    }

    private final ShipmentRepository shipmentRepository;
    private final ShipmentTrackingEventRepository trackingRepository;
    private final ShipmentMapper mapper;
    private final ShippingProviderRegistry providerRegistry;
    private final ShipmentEventPublisher eventPublisher;

    @Value("${app.shipping.default-provider:MOCK}")
    private ShippingProvider defaultProvider;

    @Override
    @Transactional
    public Shipment createFromOrder(OrderConfirmedEvent event) {
        Long orderId = Long.parseLong(event.orderId());

        // Idempotency: a duplicated order.confirmed Kafka delivery must not
        // create a second shipment. Returning the existing record lets the
        // caller mark the event processed without raising an error.
        var existing = shipmentRepository.findByOrderId(orderId);
        if (existing.isPresent()) {
            log.info("Shipment already exists for orderId={}, returning existing", orderId);
            return existing.get();
        }

        OrderConfirmedEvent.ShippingAddress addr = event.shipping();
        if (addr == null) {
            throw new InvalidShipmentStateException(
                    "order.confirmed event is missing shipping address for orderId=" + orderId);
        }

        Integer totalWeight = computeWeight(event);
        BigDecimal codAmount = computeCodAmount(event);

        ShippingProvider provider = defaultProvider;
        CreateShipmentResponse providerResponse = providerRegistry.get(provider).createShipment(
                CreateShipmentRequest.builder()
                        .orderNumber(event.orderNumber())
                        .recipientName(addr.name())
                        .recipientPhone(addr.phone())
                        .addressStreet(addr.street())
                        .addressWard(addr.ward())
                        .addressDistrict(addr.district())
                        .addressProvince(addr.province())
                        .addressCountry(addr.country() == null ? "VN" : addr.country())
                        .weightG(totalWeight)
                        .shippingFee(event.shippingFee())
                        .codAmount(codAmount)
                        .build());

        UUID userId = event.userId();
        BigDecimal shippingFee = event.shippingFee() == null ? BigDecimal.ZERO : event.shippingFee();

        Shipment shipment = Shipment.builder()
                .orderId(orderId)
                .orderNumber(event.orderNumber())
                .userId(userId)
                .provider(provider)
                .providerOrderId(providerResponse.providerOrderId())
                .trackingNumber(providerResponse.trackingNumber())
                .status(ShipmentStatus.PENDING)
                .recipientName(addr.name())
                .recipientPhone(addr.phone())
                .addressStreet(addr.street())
                .addressWard(addr.ward())
                .addressDistrict(addr.district())
                .addressProvince(addr.province())
                .addressCountry(addr.country() == null ? "VN" : addr.country())
                .weightG(totalWeight)
                .shippingFee(shippingFee)
                .codAmount(codAmount)
                .estimatedPickupAt(providerResponse.estimatedPickupAt())
                .estimatedDeliveryAt(providerResponse.estimatedDeliveryAt())
                .build();

        shipment = shipmentRepository.save(shipment);
        recordTracking(shipment, ShipmentStatus.PENDING, "Shipment created", null,
                TrackingEventSource.EVENT, null);

        // Publish so order-service can update its OrderShippingInfo with the
        // tracking number it received here.
        eventPublisher.publishShippingUpdated(shipment);

        return shipment;
    }

    @Override
    @Transactional
    public Shipment cancelByOrderId(Long orderId, String reason) {
        Shipment shipment = shipmentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ShipmentNotFoundException(
                        "No shipment for orderId=" + orderId));

        if (!shipment.getStatus().isCancellable()) {
            throw new InvalidShipmentStateException(
                    "Cannot cancel shipment in status " + shipment.getStatus());
        }

        providerRegistry.get(shipment.getProvider())
                .cancelShipment(shipment.getProviderOrderId(), reason);

        shipment.setStatus(ShipmentStatus.CANCELLED);
        shipment.setCancelledAt(LocalDateTime.now());
        shipment.setCancelReason(reason);
        shipment = shipmentRepository.save(shipment);

        recordTracking(shipment, ShipmentStatus.CANCELLED, "Cancelled: " + reason, null,
                TrackingEventSource.EVENT, null);
        eventPublisher.publishShippingUpdated(shipment);
        return shipment;
    }

    @Override
    @Transactional
    public Shipment updateStatus(Long shipmentId, ShipmentStatus newStatus, String description,
                                  String location, TrackingEventSource source, String rawPayload) {
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new ShipmentNotFoundException(
                        "Shipment not found: id=" + shipmentId));
        return applyTransition(shipment, newStatus, description, location, source, rawPayload);
    }

    @Override
    @Transactional
    public Shipment applyWebhook(String trackingNumber, ShipmentStatus status, String description,
                                  String location, String rawPayload) {
        Shipment shipment = shipmentRepository.findByTrackingNumber(trackingNumber)
                .orElseThrow(() -> new ShipmentNotFoundException(
                        "No shipment found for trackingNumber=" + trackingNumber));
        return applyTransition(shipment, status, description, location,
                TrackingEventSource.PROVIDER_WEBHOOK, rawPayload);
    }

    @Override
    @Transactional(readOnly = true)
    public ShipmentDTO getById(Long id) {
        Shipment shipment = shipmentRepository.findById(id)
                .orElseThrow(() -> new ShipmentNotFoundException("Shipment not found: id=" + id));
        return mapper.toDto(shipment, trackingRepository.findByShipmentIdOrderByOccurredAtDesc(shipment.getId()));
    }

    @Override
    @Transactional(readOnly = true)
    public ShipmentDTO getByOrderNumber(String orderNumber) {
        Shipment shipment = shipmentRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new ShipmentNotFoundException(
                        "Shipment not found for orderNumber=" + orderNumber));
        return mapper.toDto(shipment, trackingRepository.findByShipmentIdOrderByOccurredAtDesc(shipment.getId()));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ShipmentDTO> listByStatus(ShipmentStatus status, Pageable pageable) {
        Page<Shipment> page = (status == null)
                ? shipmentRepository.findAll(pageable)
                : shipmentRepository.findByStatus(status, pageable);
        return page.map(s -> mapper.toDto(s, List.of()));
    }

    private Shipment applyTransition(Shipment shipment, ShipmentStatus newStatus, String description,
                                      String location, TrackingEventSource source, String rawPayload) {
        ShipmentStatus current = shipment.getStatus();
        if (current == newStatus) {
            // Idempotent re-delivery of the same webhook event must not be an error.
            log.info("Status no-op for shipmentId={}, already {}", shipment.getId(), current);
            return shipment;
        }
        Set<ShipmentStatus> allowed = ALLOWED.getOrDefault(current, EnumSet.noneOf(ShipmentStatus.class));
        if (!allowed.contains(newStatus)) {
            throw new InvalidShipmentStateException(
                    "Invalid transition: " + current + " -> " + newStatus);
        }

        shipment.setStatus(newStatus);
        switch (newStatus) {
            case PICKED_UP -> shipment.setPickedUpAt(LocalDateTime.now());
            case DELIVERED -> shipment.setDeliveredAt(LocalDateTime.now());
            case CANCELLED -> {
                shipment.setCancelledAt(LocalDateTime.now());
                if (description != null) shipment.setCancelReason(description);
            }
            default -> { }
        }
        shipment = shipmentRepository.save(shipment);

        recordTracking(shipment, newStatus, description, location, source, rawPayload);
        eventPublisher.publishShippingUpdated(shipment);
        return shipment;
    }

    private void recordTracking(Shipment shipment, ShipmentStatus status, String description,
                                String location, TrackingEventSource source, String rawPayload) {
        trackingRepository.save(ShipmentTrackingEvent.builder()
                .shipmentId(shipment.getId())
                .status(status)
                .description(description)
                .location(location)
                .source(source)
                .rawPayload(rawPayload)
                .occurredAt(LocalDateTime.now())
                .build());
    }

    private Integer computeWeight(OrderConfirmedEvent event) {
        if (event.items() == null || event.items().isEmpty()) {
            return null;
        }
        // Best-effort: order-service does not snapshot per-item weight today, so
        // we fall back to a flat default per item until product-service exposes
        // weight via internal API. Real GHN/GHTK adapters can recompute.
        return event.items().stream().mapToInt(i -> 200 * (i.quantity() == null ? 1 : i.quantity())).sum();
    }

    private BigDecimal computeCodAmount(OrderConfirmedEvent event) {
        // COD only when payment method indicates cash-on-delivery. The
        // string-based check avoids importing PaymentMethod from order-service.
        if (event.paymentMethod() != null && event.paymentMethod().equalsIgnoreCase("COD")
                && event.totalAmount() != null) {
            return event.totalAmount();
        }
        return BigDecimal.ZERO;
    }
}
