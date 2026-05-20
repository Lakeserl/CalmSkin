package com.lakeserl.order_service.exception;

import com.lakeserl.order_service.dto.response.ApiResponse;
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

    @ExceptionHandler(OrderNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiResponse<Object> handleOrderNotFound(OrderNotFoundException ex) {
        return buildError("ORDER_NOT_FOUND", ex.getMessage(), null);
    }

    @ExceptionHandler(OrderNotBelongToUserException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ApiResponse<Object> handleOrderNotBelongToUser(OrderNotBelongToUserException ex) {
        return buildError("ORDER_ACCESS_DENIED", ex.getMessage(), null);
    }

    @ExceptionHandler(InvalidOrderStatusTransitionException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiResponse<Object> handleInvalidStatusTransition(InvalidOrderStatusTransitionException ex) {
        return buildError("INVALID_STATUS_TRANSITION", ex.getMessage(), null);
    }

    @ExceptionHandler(OrderAlreadyCancelledException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiResponse<Object> handleOrderAlreadyCancelled(OrderAlreadyCancelledException ex) {
        return buildError("ORDER_ALREADY_CANCELLED", ex.getMessage(), null);
    }

    @ExceptionHandler(OrderNotCancellableException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiResponse<Object> handleOrderNotCancellable(OrderNotCancellableException ex) {
        return buildError("ORDER_NOT_CANCELLABLE", ex.getMessage(), null);
    }

    @ExceptionHandler(ProductNotAvailableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Object> handleProductNotAvailable(ProductNotAvailableException ex) {
        return buildError("PRODUCT_NOT_AVAILABLE", ex.getMessage(), null);
    }

    @ExceptionHandler(InsufficientStockException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiResponse<Object> handleInsufficientStock(InsufficientStockException ex) {
        return buildError("INSUFFICIENT_STOCK", ex.getMessage(), null);
    }

    @ExceptionHandler(InvalidVoucherException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Object> handleInvalidVoucher(InvalidVoucherException ex) {
        return buildError("INVALID_VOUCHER", ex.getMessage(), null);
    }

    @ExceptionHandler(ServiceUnavailableException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public ApiResponse<Object> handleServiceUnavailable(ServiceUnavailableException ex) {
        return buildError("SERVICE_UNAVAILABLE", ex.getMessage(), null);
    }

    @ExceptionHandler(DuplicateOrderException.class)
    @ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
    public ApiResponse<Object> handleDuplicateOrder(DuplicateOrderException ex) {
        return buildError("DUPLICATE_ORDER", ex.getMessage(), null);
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
    public ApiResponse<Object> handleGenericException(Exception ex) {
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
