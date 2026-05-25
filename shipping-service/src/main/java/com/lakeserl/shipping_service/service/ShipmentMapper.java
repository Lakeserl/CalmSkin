package com.lakeserl.shipping_service.service;

import com.lakeserl.shipping_service.dto.response.ShipmentDTO;
import com.lakeserl.shipping_service.dto.response.TrackingEventDTO;
import com.lakeserl.shipping_service.entity.Shipment;
import com.lakeserl.shipping_service.entity.ShipmentTrackingEvent;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ShipmentMapper {

    public ShipmentDTO toDto(Shipment s, List<ShipmentTrackingEvent> events) {
        return ShipmentDTO.builder()
                .id(s.getId())
                .orderId(s.getOrderId())
                .orderNumber(s.getOrderNumber())
                .userId(s.getUserId())
                .provider(s.getProvider())
                .providerOrderId(s.getProviderOrderId())
                .trackingNumber(s.getTrackingNumber())
                .status(s.getStatus())
                .recipientName(s.getRecipientName())
                .recipientPhone(s.getRecipientPhone())
                .addressStreet(s.getAddressStreet())
                .addressWard(s.getAddressWard())
                .addressDistrict(s.getAddressDistrict())
                .addressProvince(s.getAddressProvince())
                .addressCountry(s.getAddressCountry())
                .weightG(s.getWeightG())
                .shippingFee(s.getShippingFee())
                .codAmount(s.getCodAmount())
                .estimatedPickupAt(s.getEstimatedPickupAt())
                .estimatedDeliveryAt(s.getEstimatedDeliveryAt())
                .pickedUpAt(s.getPickedUpAt())
                .deliveredAt(s.getDeliveredAt())
                .cancelledAt(s.getCancelledAt())
                .cancelReason(s.getCancelReason())
                .createdAt(s.getCreatedAt())
                .updatedAt(s.getUpdatedAt())
                .trackingEvents(events == null ? List.of() : events.stream().map(this::toEventDto).toList())
                .build();
    }

    public TrackingEventDTO toEventDto(ShipmentTrackingEvent e) {
        return TrackingEventDTO.builder()
                .id(e.getId())
                .status(e.getStatus())
                .description(e.getDescription())
                .location(e.getLocation())
                .source(e.getSource())
                .occurredAt(e.getOccurredAt())
                .build();
    }
}
