package com.lakeserl.order_service.exception;

public class InvalidVoucherException extends RuntimeException {
    public InvalidVoucherException(String message) {
        super(message);
    }
}
