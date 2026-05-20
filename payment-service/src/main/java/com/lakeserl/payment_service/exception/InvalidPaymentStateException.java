package com.lakeserl.payment_service.exception;

/** Thrown when an operation is not valid for the payment's current status. */
public class InvalidPaymentStateException extends RuntimeException {
    public InvalidPaymentStateException(String message) {
        super(message);
    }
}
