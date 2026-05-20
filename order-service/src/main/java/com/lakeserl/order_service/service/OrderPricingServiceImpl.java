package com.lakeserl.order_service.service;

import com.lakeserl.order_service.entity.OrderItem;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class OrderPricingServiceImpl implements OrderPricingService {

    @Value("${app.order.point-value:100}")
    private int pointValue; // e.g., 100 VND per point

    @Override
    public BigDecimal calculateSubtotal(List<OrderItem> items) {
        if (items == null || items.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return items.stream()
                .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    public BigDecimal calculatePointsAmount(int pointsUsed) {
        if (pointsUsed <= 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf((long) pointsUsed * pointValue);
    }

    @Override
    public BigDecimal calculateTotal(BigDecimal subtotal, BigDecimal shippingFee, BigDecimal voucherDiscount, BigDecimal pointsAmount) {
        BigDecimal baseSubtotal = subtotal == null ? BigDecimal.ZERO : subtotal;
        BigDecimal baseShippingFee = shippingFee == null ? BigDecimal.ZERO : shippingFee;
        BigDecimal baseVoucherDiscount = voucherDiscount == null ? BigDecimal.ZERO : voucherDiscount;
        BigDecimal basePointsAmount = pointsAmount == null ? BigDecimal.ZERO : pointsAmount;

        // pointsAmount must not exceed subtotal
        if (basePointsAmount.compareTo(baseSubtotal) > 0) {
            basePointsAmount = baseSubtotal;
        }

        BigDecimal total = baseSubtotal.add(baseShippingFee)
                .subtract(baseVoucherDiscount)
                .subtract(basePointsAmount);

        return total.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : total;
    }
}
