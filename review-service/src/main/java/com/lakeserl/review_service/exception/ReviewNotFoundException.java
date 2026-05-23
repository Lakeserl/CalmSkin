package com.lakeserl.review_service.exception;

public class ReviewNotFoundException extends RuntimeException {
    public ReviewNotFoundException(String message) { super(message); }
}
