package com.lakeserl.user_service.exception;

public class OtpExpiredException extends RuntimeException {
    public OtpExpiredException(String message) {
        super(message);
    }
    public OtpExpiredException() {
        super("OTP has expired");
    }
}
