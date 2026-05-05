package com.lakeserl.user_service.exception;

public class OtpInvalidException extends RuntimeException {
    public OtpInvalidException(String message) {
        super(message);
    }
    public OtpInvalidException() {
        super("OTP is invalid");
    }
}
