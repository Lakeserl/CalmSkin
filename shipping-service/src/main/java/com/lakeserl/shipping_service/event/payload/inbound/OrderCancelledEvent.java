package com.lakeserl.shipping_service.event.payload.inbound;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OrderCancelledEvent(
        String orderId,
        String reason
) {}
