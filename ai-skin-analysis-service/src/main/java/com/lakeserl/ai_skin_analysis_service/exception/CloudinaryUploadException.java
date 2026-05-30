package com.lakeserl.ai_skin_analysis_service.exception;

public class CloudinaryUploadException extends RuntimeException {
    public CloudinaryUploadException(String message) {
        super(message);
    }

    public CloudinaryUploadException(String message, Throwable cause) {
        super(message, cause);
    }
}
