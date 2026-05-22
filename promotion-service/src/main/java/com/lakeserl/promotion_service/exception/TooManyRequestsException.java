package com.lakeserl.promotion_service.exception;

/** Thrown when a caller exceeds an anti-abuse rate limit. */
public class TooManyRequestsException extends RuntimeException {
    public TooManyRequestsException(String message) {
        super(message);
    }
}
