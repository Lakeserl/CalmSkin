package com.lakeserl.order_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateOrderStatusRequest(
    @NotBlank(message = "Status is required")
    String status,

    @Size(max = 500, message = "Note must not exceed 500 characters")
    String note
) {}
