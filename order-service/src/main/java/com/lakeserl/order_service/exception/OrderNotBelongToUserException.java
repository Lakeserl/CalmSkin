package com.lakeserl.order_service.exception;

public class OrderNotBelongToUserException extends RuntimeException {
    public OrderNotBelongToUserException(String message) {
        super(message);
    }
}
