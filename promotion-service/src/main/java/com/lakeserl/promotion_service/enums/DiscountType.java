package com.lakeserl.promotion_service.enums;

/**
 * How the discount value is interpreted. For PERCENTAGE the value is a plain
 * percentage (e.g. 20 means 20%).
 */
public enum DiscountType {
    PERCENTAGE,
    FIXED_AMOUNT,
    FREE_SHIPPING,
    FREE_GIFT
}
