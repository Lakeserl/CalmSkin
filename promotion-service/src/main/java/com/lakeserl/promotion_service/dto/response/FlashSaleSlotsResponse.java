package com.lakeserl.promotion_service.dto.response;

/** Real-time slot counters for one flash-sale line. */
public record FlashSaleSlotsResponse(
        Long flashSaleId,
        Long productId,
        Long variantId,
        Integer quantityLimit,
        Integer quantitySold,
        Integer quantityReserved,
        Integer quantityLeft
) {
}
