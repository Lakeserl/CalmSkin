package com.lakeserl.promotion_service.exception;

/** Thrown when a promotion, voucher or flash sale does not exist. */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
