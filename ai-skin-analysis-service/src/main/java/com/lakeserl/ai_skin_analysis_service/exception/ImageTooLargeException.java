package com.lakeserl.ai_skin_analysis_service.exception;

public class ImageTooLargeException extends RuntimeException {
    public ImageTooLargeException(String message) {
        super(message);
    }
}
