package com.lakeserl.shipping_service.event.payload.inbound;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

// Mirrors the additive payload published by order-service
// (InventoryEventConsumer.buildOrderConfirmedPayload). Unknown fields are
// ignored so we tolerate future additive changes from upstream.
@JsonIgnoreProperties(ignoreUnknown = true)
public record OrderConfirmedEvent(
        String orderId,
        String orderNumber,
        UUID userId,
        BigDecimal totalAmount,
        String paymentMethod,
        BigDecimal shippingFee,
        ShippingAddress shipping,
        List<Item> items
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ShippingAddress(
            String name,
            String phone,
            String street,
            String ward,
            String district,
            String province,
            String country
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Item(
            Long orderItemId,
            Long productId,
            Long variantId,
            String productName,
            String productSku,
            Integer quantity,
            BigDecimal unitPrice,
            BigDecimal subtotal
    ) {}
}
