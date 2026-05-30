package com.lakeserl.ai_skin_analysis_service.exception;

public class AIServiceUnavailableException extends RuntimeException {
    public AIServiceUnavailableException(String message) {
        super(message);
    }

    public AIServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
