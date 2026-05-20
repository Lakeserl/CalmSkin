package com.lakeserl.payment_service.exception;

/** Thrown when a payment already exists for an order. */
public class DuplicatePaymentException extends RuntimeException {
    public DuplicatePaymentException(String message) {
        super(message);
    }
}
