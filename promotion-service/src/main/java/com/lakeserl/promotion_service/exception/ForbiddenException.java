package com.lakeserl.promotion_service.exception;

/** Thrown when the caller is authenticated but not allowed to touch the resource. */
public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }
}
