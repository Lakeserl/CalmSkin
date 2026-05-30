package com.lakeserl.ai_skin_analysis_service.exception;

public class SessionNotFoundException extends RuntimeException {
    public SessionNotFoundException(String message) {
        super(message);
    }
}
