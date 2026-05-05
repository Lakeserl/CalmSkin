package com.lakeserl.user_service.exception;

public class TokenInvalidException extends RuntimeException {
    public TokenInvalidException(String message) {
        super(message);
    }
    public TokenInvalidException() {
        super("Token is invalid");
    }
}
