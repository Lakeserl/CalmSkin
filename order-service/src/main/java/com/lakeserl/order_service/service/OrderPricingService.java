package com.lakeserl.order_service.service;

import com.lakeserl.order_service.entity.OrderItem;
import java.math.BigDecimal;
import java.util.List;

public interface OrderPricingService {
    BigDecimal calculateSubtotal(List<OrderItem> items);
    
    BigDecimal calculatePointsAmount(int pointsUsed);
    
    BigDecimal calculateTotal(BigDecimal subtotal, BigDecimal shippingFee, BigDecimal voucherDiscount, BigDecimal pointsAmount);
}
