package com.lakeserl.user_service.exception;

public class AccountNotVerifiedException extends RuntimeException {
    public AccountNotVerifiedException(String message) {
        super(message);
    }
    public AccountNotVerifiedException() {
        super("Account is not verified");
    }
}
