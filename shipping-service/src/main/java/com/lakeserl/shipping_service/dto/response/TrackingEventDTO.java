package com.lakeserl.shipping_service.dto.response;

import com.lakeserl.shipping_service.enums.ShipmentStatus;
import com.lakeserl.shipping_service.enums.TrackingEventSource;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class TrackingEventDTO {
    private Long id;
    private ShipmentStatus status;
    private String description;
    private String location;
    private TrackingEventSource source;
    private LocalDateTime occurredAt;
}
