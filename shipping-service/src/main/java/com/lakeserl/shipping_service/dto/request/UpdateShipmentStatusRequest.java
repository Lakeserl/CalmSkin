package com.lakeserl.shipping_service.dto.request;

import com.lakeserl.shipping_service.enums.ShipmentStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateShipmentStatusRequest(
        @NotNull ShipmentStatus status,
        String description,
        String location
) {}
