package com.lakeserl.inventory_service.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ConfirmStockRequest(@NotBlank String orderId) {
}
