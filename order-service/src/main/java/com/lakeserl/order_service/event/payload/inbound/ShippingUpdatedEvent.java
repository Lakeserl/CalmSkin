package com.lakeserl.order_service.event.payload.inbound;

public record ShippingUpdatedEvent(
    String orderId,
    String shippingStatus,
    String trackingNumber,
    String shippingProvider,
    String estimatedDelivery
) {}
