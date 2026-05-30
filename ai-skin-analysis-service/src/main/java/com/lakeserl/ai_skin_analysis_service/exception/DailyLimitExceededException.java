package com.lakeserl.ai_skin_analysis_service.exception;

public class DailyLimitExceededException extends RuntimeException {
    public DailyLimitExceededException(String message) {
        super(message);
    }
}
