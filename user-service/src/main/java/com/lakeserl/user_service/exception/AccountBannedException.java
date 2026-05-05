package com.lakeserl.user_service.exception;

public class AccountBannedException extends RuntimeException {
    public AccountBannedException(String message) {
        super(message);
    }
    public AccountBannedException() {
        super("Account has been banned");
    }
}
