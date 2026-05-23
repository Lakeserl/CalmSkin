package com.lakeserl.review_service.exception;

public class NotEligibleToReviewException extends RuntimeException {
    public NotEligibleToReviewException(String message) { super(message); }
}
