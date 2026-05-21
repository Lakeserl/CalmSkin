package com.lakeserl.notification_service.exception;

/** Thrown when a requested resource (notification, preference, ...) does not exist. */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
