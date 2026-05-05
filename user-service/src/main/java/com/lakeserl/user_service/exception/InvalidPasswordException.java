package com.lakeserl.user_service.exception;

public class InvalidPasswordException extends RuntimeException {
    public InvalidPasswordException(String message) {
        super(message);
    }
    public InvalidPasswordException() {
        super("Invalid password");
    }
}
