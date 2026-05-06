package com.lakeserl.product_service.exception;

public class InvalidProductStatusException extends RuntimeException {
    public InvalidProductStatusException(String message) {
        super(message);
    }
}
