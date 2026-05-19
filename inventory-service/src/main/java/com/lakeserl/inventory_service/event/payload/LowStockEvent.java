package com.lakeserl.inventory_service.event.payload;

public record LowStockEvent(Long productId, Long variantId, int quantityAvailable, int lowStockThreshold) {
}
