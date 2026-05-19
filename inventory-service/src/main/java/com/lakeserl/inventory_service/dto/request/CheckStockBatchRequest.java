package com.lakeserl.inventory_service.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record CheckStockBatchRequest(@NotEmpty List<@Valid CheckStockRequest> items) {
}
