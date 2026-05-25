package com.lakeserl.shipping_service.provider;

import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record CreateShipmentRequest(
        String orderNumber,
        String recipientName,
        String recipientPhone,
        String addressStreet,
        String addressWard,
        String addressDistrict,
        String addressProvince,
        String addressCountry,
        Integer weightG,
        BigDecimal shippingFee,
        BigDecimal codAmount
) {}
