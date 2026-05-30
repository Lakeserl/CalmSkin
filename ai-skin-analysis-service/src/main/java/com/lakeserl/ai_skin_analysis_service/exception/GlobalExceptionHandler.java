package com.lakeserl.ai_skin_analysis_service.exception;

import com.lakeserl.ai_skin_analysis_service.dto.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BiometricConsentMissingException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ApiResponse<Void> handleConsentMissing(BiometricConsentMissingException ex) {
        return ApiResponse.error("BIOMETRIC_CONSENT_REQUIRED", ex.getMessage());
    }

    @ExceptionHandler(NoFaceDetectedException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleNoFaceDetected(NoFaceDetectedException ex) {
        return ApiResponse.error("NO_FACE_DETECTED", ex.getMessage());
    }

    @ExceptionHandler(ImageTooLargeException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleImageTooLarge(ImageTooLargeException ex) {
        return ApiResponse.error("IMAGE_TOO_LARGE", ex.getMessage());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleMaxUploadSize(MaxUploadSizeExceededException ex) {
        return ApiResponse.error("IMAGE_TOO_LARGE", "Image exceeds maximum allowed size of 10MB");
    }

    @ExceptionHandler(InvalidImageFormatException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleInvalidImageFormat(InvalidImageFormatException ex) {
        return ApiResponse.error("INVALID_IMAGE_FORMAT", ex.getMessage());
    }

    @ExceptionHandler(SessionNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiResponse<Void> handleSessionNotFound(SessionNotFoundException ex) {
        return ApiResponse.error("SESSION_NOT_FOUND", ex.getMessage());
    }

    @ExceptionHandler(DailyLimitExceededException.class)
    @ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
    public ApiResponse<Void> handleDailyLimit(DailyLimitExceededException ex) {
        return ApiResponse.error("DAILY_LIMIT_EXCEEDED", ex.getMessage());
    }

    @ExceptionHandler(AIServiceUnavailableException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public ApiResponse<Void> handleAIServiceUnavailable(AIServiceUnavailableException ex) {
        log.error("AI service unavailable: {}", ex.getMessage());
        return ApiResponse.error("AI_SERVICE_UNAVAILABLE", "AI analysis service is temporarily unavailable. Please try again later.");
    }

    @ExceptionHandler(CloudinaryUploadException.class)
    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    public ApiResponse<Void> handleCloudinaryUpload(CloudinaryUploadException ex) {
        log.error("Cloudinary upload failed: {}", ex.getMessage());
        return ApiResponse.error("IMAGE_UPLOAD_FAILED", "Image upload failed. Please try again.");
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ApiResponse<Void> handleAccessDenied(AccessDeniedException ex) {
        return ApiResponse.error("ACCESS_DENIED", ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> handleGeneric(Exception ex) {
        log.error("Unhandled exception: {}", ex.getMessage(), ex);
        return ApiResponse.error("INTERNAL_ERROR", "An unexpected error occurred. Please try again.");
    }
}
