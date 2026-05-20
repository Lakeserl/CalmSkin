package com.lakeserl.payment_service.exception;

import com.lakeserl.payment_service.models.enums.PaymentMethod;

/** Thrown when no gateway is registered for a requested payment method. */
public class UnsupportedPaymentMethodException extends RuntimeException {
    public UnsupportedPaymentMethodException(PaymentMethod method) {
        super("No payment gateway registered for method: " + method);
    }
}
