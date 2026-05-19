package com.lakeserl.inventory_service.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ReleaseStockRequest(@NotBlank String orderId) {
}
