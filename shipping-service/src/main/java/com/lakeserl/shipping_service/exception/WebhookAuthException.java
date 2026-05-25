package com.lakeserl.shipping_service.exception;

public class WebhookAuthException extends RuntimeException {
    public WebhookAuthException(String message) {
        super(message);
    }
}
