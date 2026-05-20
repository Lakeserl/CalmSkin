package com.lakeserl.payment_service.exception;

/** Thrown when a refund cannot be created or processed. */
public class RefundException extends RuntimeException {
    public RefundException(String message) {
        super(message);
    }
}
