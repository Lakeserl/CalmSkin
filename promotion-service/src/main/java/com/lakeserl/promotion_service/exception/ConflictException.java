package com.lakeserl.promotion_service.exception;

/**
 * Thrown when a request conflicts with current state - e.g. a usage limit is
 * exhausted, a flash sale slot is gone, or a lock already exists.
 */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}
