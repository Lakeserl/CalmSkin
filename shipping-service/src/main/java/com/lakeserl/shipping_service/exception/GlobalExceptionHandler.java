package com.lakeserl.shipping_service.exception;

import com.lakeserl.shipping_service.dto.response.ApiResponse;
import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ShipmentNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiResponse<Object> handleShipmentNotFound(ShipmentNotFoundException ex) {
        return buildError("SHIPMENT_NOT_FOUND", ex.getMessage(), null);
    }

    @ExceptionHandler(DuplicateShipmentException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiResponse<Object> handleDuplicate(DuplicateShipmentException ex) {
        return buildError("DUPLICATE_SHIPMENT", ex.getMessage(), null);
    }

    @ExceptionHandler(InvalidShipmentStateException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiResponse<Object> handleInvalidState(InvalidShipmentStateException ex) {
        return buildError("INVALID_SHIPMENT_STATE", ex.getMessage(), null);
    }

    @ExceptionHandler(WebhookAuthException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ApiResponse<Object> handleWebhookAuth(WebhookAuthException ex) {
        return buildError("WEBHOOK_AUTH_FAILED", ex.getMessage(), null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Object> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        error -> error.getDefaultMessage() == null ? "Invalid value" : error.getDefaultMessage(),
                        (existing, replacement) -> existing
                ));
        return buildError("VALIDATION_FAILED", "Validation failed", errors);
    }

    @ExceptionHandler({MissingServletRequestParameterException.class, MethodArgumentTypeMismatchException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Object> handleBadRequest(Exception ex) {
        return buildError("INVALID_REQUEST", ex.getMessage(), null);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Object> handleNotReadable(HttpMessageNotReadableException ex) {
        return buildError("INVALID_REQUEST_BODY", "Request body is invalid", null);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Object> handleGeneric(Exception ex) {
        return buildError("INTERNAL_SERVER_ERROR", "An unexpected error occurred", null);
    }

    private ApiResponse<Object> buildError(String code, String message, Object data) {
        return ApiResponse.builder()
                .success(false)
                .code(code)
                .message(message)
                .data(data)
                .timestamp(Instant.now())
                .requestId(MDC.get("requestId"))
                .path(MDC.get("path"))
                .build();
    }
}
