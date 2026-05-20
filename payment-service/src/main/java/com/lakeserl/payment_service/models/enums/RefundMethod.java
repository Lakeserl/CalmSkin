package com.lakeserl.payment_service.models.enums;

/**
 * How a refund is processed.
 * ORIGINAL — refund via the same gateway that handled the payment.
 * BANK_TRANSFER — manual bank transfer (e.g. VNPay after T+0).
 * POINTS — credit to user loyalty balance.
 */
public enum RefundMethod {
    ORIGINAL,
    BANK_TRANSFER,
    POINTS
}
