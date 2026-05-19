package com.lakeserl.inventory_service.event.payload;

public record OutOfStockEvent(Long productId, Long variantId, int quantityAvailable) {
}
