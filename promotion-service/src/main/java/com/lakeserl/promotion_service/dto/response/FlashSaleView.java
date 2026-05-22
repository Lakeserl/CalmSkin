package com.lakeserl.promotion_service.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** A flash-sale promotion and its product lines, for public browsing. */
public record FlashSaleView(
        Long promotionId,
        String promotionName,
        LocalDateTime startsAt,
        LocalDateTime endsAt,
        List<Product> products
) {

    public record Product(
            Long productId,
            Long variantId,
            BigDecimal salePrice,
            BigDecimal originalPrice,
            Integer quantityLeft
    ) {
    }
}
