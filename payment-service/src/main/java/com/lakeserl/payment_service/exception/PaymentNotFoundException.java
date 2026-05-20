package com.lakeserl.payment_service.exception;

/** Thrown when a payment cannot be located by number, id or order id. */
public class PaymentNotFoundException extends RuntimeException {
    public PaymentNotFoundException(String message) {
        super(message);
    }
}
