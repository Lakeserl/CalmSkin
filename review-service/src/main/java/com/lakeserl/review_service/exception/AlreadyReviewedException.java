package com.lakeserl.review_service.exception;

public class AlreadyReviewedException extends RuntimeException {
    public AlreadyReviewedException(String message) { super(message); }
}
