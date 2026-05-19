package com.lakeserl.inventory_service.exception;

public class DuplicateInventoryException extends RuntimeException {
    public DuplicateInventoryException(String message) {
        super(message);
    }

    public DuplicateInventoryException(String message, Throwable cause) {
        super(message, cause);
    }
}
