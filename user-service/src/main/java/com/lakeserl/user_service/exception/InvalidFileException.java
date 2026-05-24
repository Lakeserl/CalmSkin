package com.lakeserl.user_service.exception;

public class InvalidFileException extends RuntimeException {
    public InvalidFileException(String message) {
        super(message);
    }
    public InvalidFileException() {
        super("Invalid file");
    }
}
