package com.lakeserl.notification_service.exception;

/** Thrown when a request is structurally valid but breaks a business rule. */
public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }
}
