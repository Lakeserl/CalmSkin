package com.lakeserl.user_service.handler;

import com.lakeserl.user_service.model.dto.response.ErrorResponse;
import com.lakeserl.user_service.exception.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import java.time.LocalDateTime;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({UserNotFoundException.class})
    public ResponseEntity<ErrorResponse> handleUserNotFound(UserNotFoundException ex, HttpServletRequest request) {
        return buildErrorResponse(false, "USER_NOT_FOUND", ex.getMessage(), request.getRequestURI(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler({EmailAlreadyExistsException.class})
    public ResponseEntity<ErrorResponse> handleEmailExists(EmailAlreadyExistsException ex, HttpServletRequest request) {
        return buildErrorResponse(false, "EMAIL_ALREADY_EXISTS", ex.getMessage(), request.getRequestURI(), HttpStatus.CONFLICT);
    }

    @ExceptionHandler({PhoneAlreadyExistsException.class})
    public ResponseEntity<ErrorResponse> handlePhoneExists(PhoneAlreadyExistsException ex, HttpServletRequest request) {
        return buildErrorResponse(false, "PHONE_ALREADY_EXISTS", ex.getMessage(), request.getRequestURI(), HttpStatus.CONFLICT);
    }

    @ExceptionHandler({InvalidCredentialsException.class})
    public ResponseEntity<ErrorResponse> handleInvalidCredentials(InvalidCredentialsException ex, HttpServletRequest request) {
        return buildErrorResponse(false, "INVALID_CREDENTIALS", ex.getMessage(), request.getRequestURI(), HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler({AccountNotVerifiedException.class})
    public ResponseEntity<ErrorResponse> handleAccountNotVerified(AccountNotVerifiedException ex, HttpServletRequest request) {
        return buildErrorResponse(false, "ACCOUNT_NOT_VERIFIED", ex.getMessage(), request.getRequestURI(), HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler({AccountBannedException.class})
    public ResponseEntity<ErrorResponse> handleAccountBanned(AccountBannedException ex, HttpServletRequest request) {
        return buildErrorResponse(false, "ACCOUNT_BANNED", ex.getMessage(), request.getRequestURI(), HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler({TokenExpiredException.class})
    public ResponseEntity<ErrorResponse> handleTokenExpired(TokenExpiredException ex, HttpServletRequest request) {
        return buildErrorResponse(false, "TOKEN_EXPIRED", ex.getMessage(), request.getRequestURI(), HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler({TokenInvalidException.class})
    public ResponseEntity<ErrorResponse> handleTokenInvalid(TokenInvalidException ex, HttpServletRequest request) {
        return buildErrorResponse(false, "TOKEN_INVALID", ex.getMessage(), request.getRequestURI(), HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler({OtpInvalidException.class})
    public ResponseEntity<ErrorResponse> handleOtpInvalid(OtpInvalidException ex, HttpServletRequest request) {
        return buildErrorResponse(false, "OTP_INVALID", ex.getMessage(), request.getRequestURI(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler({OtpExpiredException.class})
    public ResponseEntity<ErrorResponse> handleOtpExpired(OtpExpiredException ex, HttpServletRequest request) {
        return buildErrorResponse(false, "OTP_EXPIRED", ex.getMessage(), request.getRequestURI(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler({MaxAddressLimitException.class})
    public ResponseEntity<ErrorResponse> handleMaxAddressLimit(MaxAddressLimitException ex, HttpServletRequest request) {
        return buildErrorResponse(false, "MAX_ADDRESS_LIMIT", ex.getMessage(), request.getRequestURI(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler({InvalidPasswordException.class})
    public ResponseEntity<ErrorResponse> handleInvalidPassword(InvalidPasswordException ex, HttpServletRequest request) {
        return buildErrorResponse(false, "INVALID_PASSWORD", ex.getMessage(), request.getRequestURI(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler({InvalidFileException.class})
    public ResponseEntity<ErrorResponse> handleInvalidFile(InvalidFileException ex, HttpServletRequest request) {
        return buildErrorResponse(false, "INVALID_FILE", ex.getMessage(), request.getRequestURI(), HttpStatus.UNSUPPORTED_MEDIA_TYPE);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .findFirst().orElse("Validation error");
        return buildErrorResponse(false, "VALIDATION_ERROR", message, request.getRequestURI(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleOther(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception at {}: {}", request.getRequestURI(), ex.getMessage(), ex);
        return buildErrorResponse(false, "INTERNAL_SERVER_ERROR", "An internal error occurred. Please try again.", request.getRequestURI(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private ResponseEntity<ErrorResponse> buildErrorResponse(boolean success, String code, String message, String path, HttpStatus status) {
        ErrorResponse error = ErrorResponse.builder()
                .success(success)
                .code(code)
                .message(message)
                .timestamp(LocalDateTime.now())
                .path(path)
                .build();
        return new ResponseEntity<>(error, status);
    }
}
