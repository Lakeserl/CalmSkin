package com.lakeserl.shipping_service.dto.response;

import com.lakeserl.shipping_service.enums.ShipmentStatus;
import com.lakeserl.shipping_service.enums.ShippingProvider;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class ShipmentDTO {
    private Long id;
    private Long orderId;
    private String orderNumber;
    private UUID userId;

    private ShippingProvider provider;
    private String providerOrderId;
    private String trackingNumber;
    private ShipmentStatus status;

    private String recipientName;
    private String recipientPhone;
    private String addressStreet;
    private String addressWard;
    private String addressDistrict;
    private String addressProvince;
    private String addressCountry;

    private Integer weightG;
    private BigDecimal shippingFee;
    private BigDecimal codAmount;

    private LocalDateTime estimatedPickupAt;
    private LocalDateTime estimatedDeliveryAt;
    private LocalDateTime pickedUpAt;
    private LocalDateTime deliveredAt;
    private LocalDateTime cancelledAt;
    private String cancelReason;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private List<TrackingEventDTO> trackingEvents;
}
