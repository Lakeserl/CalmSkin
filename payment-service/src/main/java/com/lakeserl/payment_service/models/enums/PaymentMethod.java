package com.lakeserl.payment_service.models.enums;

/**
 * Supported payment methods. Online gateways (VNPAY, MOMO, ZALOPAY) run in
 * sandbox/test mode only. COD is collected on delivery; POINTS settles the
 * order with the user's loyalty balance.
 */
public enum PaymentMethod {
    VNPAY,
    MOMO,
    ZALOPAY,
    COD,
    POINTS
}
