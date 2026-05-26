package com.lakeserl.subscription_service.exception;

public class SubscriptionAccessDeniedException extends RuntimeException {
    public SubscriptionAccessDeniedException(String message) {
        super(message);
    }
}
