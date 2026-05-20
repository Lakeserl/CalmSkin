package com.lakeserl.payment_service.exception;

import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.lakeserl.payment_service.models.dto.response.ApiResponse;

import lombok.extern.slf4j.Slf4j;

/**
 * Translates exceptions into the standard ApiResponse error envelope.
 * Never exposes stack traces to clients.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(PaymentNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiResponse<Object> handlePaymentNotFound(PaymentNotFoundException ex) {
        return buildError("PAYMENT_NOT_FOUND", ex.getMessage(), null);
    }

    @ExceptionHandler(DuplicatePaymentException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiResponse<Object> handleDuplicatePayment(DuplicatePaymentException ex) {
        return buildError("DUPLICATE_PAYMENT", ex.getMessage(), null);
    }

    @ExceptionHandler(InvalidPaymentStateException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiResponse<Object> handleInvalidState(InvalidPaymentStateException ex) {
        return buildError("INVALID_PAYMENT_STATE", ex.getMessage(), null);
    }

    @ExceptionHandler(UnsupportedPaymentMethodException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Object> handleUnsupportedMethod(UnsupportedPaymentMethodException ex) {
        return buildError("UNSUPPORTED_PAYMENT_METHOD", ex.getMessage(), null);
    }

    @ExceptionHandler(RefundException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiResponse<Object> handleRefund(RefundException ex) {
        return buildError("REFUND_ERROR", ex.getMessage(), null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Object> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        error -> error.getField(),
                        error -> error.getDefaultMessage() == null ? "Invalid value" : error.getDefaultMessage(),
                        (existing, replacement) -> existing));
        return buildError("VALIDATION_ERROR", "Validation failed", errors);
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
        log.error("Unhandled exception", ex);
        return buildError("INTERNAL_SERVER_ERROR", "An unexpected error occurred", null);
    }

    private ApiResponse<Object> buildError(String code, String message, Object data) {
        return ApiResponse.builder()
                .success(false)
                .code(code)
                .message(message)
                .data(data)
                .page(null)
                .timestamp(Instant.now())
                .requestId(MDC.get("requestId"))
                .path(MDC.get("path"))
                .build();
    }
}
