package com.lakeserl.order_service.event.payload.inbound;

import java.util.List;

public record InventoryReservedEvent(
    String orderId,
    List<Long> reservationIds,
    String expiresAt
) {}
