package com.lakeserl.inventory_service.exception;

public class ReservationAlreadyProcessedException extends RuntimeException {
    public ReservationAlreadyProcessedException(String message) {
        super(message);
    }

    public ReservationAlreadyProcessedException(String message, Throwable cause) {
        super(message, cause);
    }
}
