package com.lakeserl.review_service.exception;

public class MediaLimitExceededException extends RuntimeException {
    public MediaLimitExceededException(String message) { super(message); }
}
