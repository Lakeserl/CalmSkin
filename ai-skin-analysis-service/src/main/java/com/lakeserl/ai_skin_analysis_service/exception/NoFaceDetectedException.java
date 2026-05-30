package com.lakeserl.ai_skin_analysis_service.exception;

public class NoFaceDetectedException extends RuntimeException {
    public NoFaceDetectedException(String message) {
        super(message);
    }
}
