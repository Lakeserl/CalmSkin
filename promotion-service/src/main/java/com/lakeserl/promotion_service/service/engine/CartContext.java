package com.lakeserl.promotion_service.service.engine;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.lakeserl.promotion_service.dto.request.CartItemDto;

/**
 * Immutable input to the promotion engine: who is buying, what is in the cart,
 * which voucher codes were entered, and the running totals.
 */
public record CartContext(
        UUID userId,
        List<CartItemDto> cartItems,
        List<String> voucherCodes,
        BigDecimal cartTotal,
        BigDecimal shippingFee
) {

    public boolean hasCartItems() {
        return cartItems != null && !cartItems.isEmpty();
    }

    public int totalQuantity() {
        if (cartItems == null) {
            return 0;
        }
        return cartItems.stream()
                .mapToInt(i -> i.quantity() == null ? 0 : i.quantity())
                .sum();
    }

    public Set<Long> productIds() {
        if (cartItems == null) {
            return Set.of();
        }
        return cartItems.stream()
                .map(CartItemDto::productId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    public BigDecimal safeCartTotal() {
        return cartTotal == null ? BigDecimal.ZERO : cartTotal;
    }

    public BigDecimal safeShippingFee() {
        return shippingFee == null ? BigDecimal.ZERO : shippingFee;
    }
}
