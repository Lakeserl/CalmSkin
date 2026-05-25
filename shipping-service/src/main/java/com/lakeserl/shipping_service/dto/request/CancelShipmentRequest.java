package com.lakeserl.shipping_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CancelShipmentRequest(
        @NotBlank @Size(max = 500) String reason
) {}
