package com.lakeserl.user_service.exception;

public class TokenExpiredException extends RuntimeException {
    public TokenExpiredException(String message) {
        super(message);
    }
    public TokenExpiredException() {
        super("Token has expired");
    }
}
