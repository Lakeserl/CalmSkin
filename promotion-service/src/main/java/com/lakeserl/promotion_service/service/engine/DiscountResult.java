package com.lakeserl.promotion_service.service.engine;

import java.math.BigDecimal;
import java.util.List;

import com.lakeserl.promotion_service.enums.DiscountType;
import com.lakeserl.promotion_service.enums.PromotionType;

/**
 * Output of the promotion engine for a cart: the promotions that applied, the
 * total monetary and shipping discount, the flash-sale prices for cart items,
 * the free gifts earned, and human-readable warnings for promotions that did
 * not apply.
 */
public record DiscountResult(
        List<AppliedPromotion> appliedPromotions,
        BigDecimal totalDiscount,
        BigDecimal shippingDiscount,
        List<FlashSalePriceLine> flashSalePrices,
        List<GiftLine> freeGifts,
        List<String> warnings
) {

    public record AppliedPromotion(
            Long promotionId,
            String name,
            BigDecimal discountAmount,
            DiscountType discountType,
            PromotionType type
    ) {
    }

    public record FlashSalePriceLine(
            Long productId,
            Long variantId,
            BigDecimal originalPrice,
            BigDecimal salePrice
    ) {
    }

    /** A product granted free by an applied FREE_GIFT promotion. */
    public record GiftLine(
            Long productId,
            Long variantId,
            Integer quantity
    ) {
    }
}
