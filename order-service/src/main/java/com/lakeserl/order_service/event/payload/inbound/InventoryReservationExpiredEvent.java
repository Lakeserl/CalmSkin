package com.lakeserl.order_service.event.payload.inbound;

public record InventoryReservationExpiredEvent(
    String orderId
) {}
