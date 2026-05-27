package com.lakeserl.shipping_service.service;

import com.lakeserl.shipping_service.dto.response.ShipmentDTO;
import com.lakeserl.shipping_service.entity.Shipment;
import com.lakeserl.shipping_service.enums.ShipmentStatus;
import com.lakeserl.shipping_service.enums.TrackingEventSource;
import com.lakeserl.shipping_service.event.payload.inbound.OrderConfirmedEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ShipmentService {

    // Idempotent: returns the existing shipment if one already exists for the order.
    Shipment createFromOrder(OrderConfirmedEvent event);

    Shipment cancelByOrderId(Long orderId, String reason);

    Shipment updateStatus(Long shipmentId,
                          ShipmentStatus newStatus,
                          String description,
                          String location,
                          TrackingEventSource source,
                          String rawPayload);

    Shipment applyWebhook(String trackingNumber,
                          ShipmentStatus status,
                          String description,
                          String location,
                          String rawPayload);

    ShipmentDTO getById(Long id);

    ShipmentDTO getByOrderNumber(String orderNumber);

    Page<ShipmentDTO> listByStatus(ShipmentStatus status, Pageable pageable);

    Page<ShipmentDTO> listByUserId(UUID userId, Pageable pageable);

    // Ownership-scoped: throws ShipmentNotFoundException if not found or userId doesn't match
    ShipmentDTO getByOrderNumberForUser(String orderNumber, UUID userId);
}
