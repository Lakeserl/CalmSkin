package com.lakeserl.user_service.exception;

public class PhoneAlreadyExistsException extends RuntimeException {
    public PhoneAlreadyExistsException(String message) {
        super(message);
    }
    public PhoneAlreadyExistsException() {
        super("Phone number already exists");
    }
}
