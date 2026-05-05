package com.lakeserl.user_service.exception;

public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException(String message) {
        super(message);
    }
    public InvalidCredentialsException() {
        super("Invalid credentials");
    }
}
