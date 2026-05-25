package com.lakeserl.shipping_service.exception;

public class DuplicateShipmentException extends RuntimeException {
    public DuplicateShipmentException(String message) {
        super(message);
    }
}
