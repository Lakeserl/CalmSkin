package com.lakeserl.user_service.exception;

public class MaxAddressLimitException extends RuntimeException {
    public MaxAddressLimitException(String message) {
        super(message);
    }
    public MaxAddressLimitException() {
        super("Exceeded maximum address limit");
    }
}
