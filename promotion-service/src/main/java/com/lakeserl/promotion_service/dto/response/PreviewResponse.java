package com.lakeserl.promotion_service.dto.response;

import java.math.BigDecimal;
import java.util.List;

/** Result of a cart preview. */
public record PreviewResponse(
        List<ApplicablePromotion> applicablePromotions,
        List<FlashSalePrice> flashSalePrices,
        List<GiftItem> freeGifts,
        BigDecimal totalDiscount,
        BigDecimal discountedShipping,
        BigDecimal finalTotal,
        List<String> warnings
) {

    public record ApplicablePromotion(
            Long promotionId,
            String name,
            BigDecimal discountAmount,
            String discountType,
            String type
    ) {
    }

    public record FlashSalePrice(
            Long productId,
            Long variantId,
            BigDecimal originalPrice,
            BigDecimal salePrice
    ) {
    }

    /** A free product the cart has earned from a FREE_GIFT promotion. */
    public record GiftItem(
            Long productId,
            Long variantId,
            Integer quantity
    ) {
    }
}
