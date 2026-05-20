package com.lakeserl.order_service.event.payload.inbound;

public record InventoryInsufficientEvent(
    String orderId,
    String reason
) {}
