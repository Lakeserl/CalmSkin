package com.lakeserl.order_service.exception;

public class InvalidOrderStatusTransitionException extends RuntimeException {
    public InvalidOrderStatusTransitionException(String from, String to) {
        super("Transition from " + from + " to " + to + " is invalid");
    }
}
