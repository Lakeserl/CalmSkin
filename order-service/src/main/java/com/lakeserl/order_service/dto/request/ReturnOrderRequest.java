package com.lakeserl.order_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record ReturnOrderRequest(
    @NotBlank(message = "Return reason is required")
    @Size(max = 500, message = "Return reason must not exceed 500 characters")
    String reason,

    @NotEmpty(message = "Return items list must not be empty")
    List<ReturnItemRequest> items
) {
    public record ReturnItemRequest(
        Long orderItemId,
        Integer quantity
    ) {}
}
