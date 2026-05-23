package com.lakeserl.product_service.exception;

import com.lakeserl.product_service.dto.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ============ 404 NOT FOUND ============
    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleProductNotFound(ProductNotFoundException ex, HttpServletRequest request) {
        return buildErrorResponse("PRODUCT_NOT_FOUND", ex.getMessage(), request, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(CategoryNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleCategoryNotFound(CategoryNotFoundException ex, HttpServletRequest request) {
        return buildErrorResponse("CATEGORY_NOT_FOUND", ex.getMessage(), request, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(BrandNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleBrandNotFound(BrandNotFoundException ex, HttpServletRequest request) {
        return buildErrorResponse("BRAND_NOT_FOUND", ex.getMessage(), request, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(IngredientNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleIngredientNotFound(IngredientNotFoundException ex, HttpServletRequest request) {
        return buildErrorResponse("INGREDIENT_NOT_FOUND", ex.getMessage(), request, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(TagNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleTagNotFound(TagNotFoundException ex, HttpServletRequest request) {
        return buildErrorResponse("TAG_NOT_FOUND", ex.getMessage(), request, HttpStatus.NOT_FOUND);
    }

    // ============ 409 CONFLICT ============
    @ExceptionHandler(DuplicateSkuException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateSku(DuplicateSkuException ex, HttpServletRequest request) {
        return buildErrorResponse("DUPLICATE_SKU", ex.getMessage(), request, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(DuplicateSlugException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateSlug(DuplicateSlugException ex, HttpServletRequest request) {
        return buildErrorResponse("DUPLICATE_SLUG", ex.getMessage(), request, HttpStatus.CONFLICT);
    }

    // ============ 400 BAD REQUEST ============
    @ExceptionHandler(ProductImageLimitException.class)
    public ResponseEntity<ErrorResponse> handleProductImageLimit(ProductImageLimitException ex, HttpServletRequest request) {
        return buildErrorResponse("PRODUCT_IMAGE_LIMIT", ex.getMessage(), request, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ProductVariantLimitException.class)
    public ResponseEntity<ErrorResponse> handleProductVariantLimit(ProductVariantLimitException ex, HttpServletRequest request) {
        return buildErrorResponse("PRODUCT_VARIANT_LIMIT", ex.getMessage(), request, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(InvalidProductStatusException.class)
    public ResponseEntity<ErrorResponse> handleInvalidProductStatus(InvalidProductStatusException ex, HttpServletRequest request) {
        return buildErrorResponse("INVALID_PRODUCT_STATUS", ex.getMessage(), request, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ProductNotActiveException.class)
    public ResponseEntity<ErrorResponse> handleProductNotActive(ProductNotActiveException ex, HttpServletRequest request) {
        return buildErrorResponse("PRODUCT_NOT_ACTIVE", ex.getMessage(), request, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<String> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .toList();
        return buildErrorResponse("VALIDATION_ERROR", String.join(", ", errors), request, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleNotReadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
        return buildErrorResponse("INVALID_REQUEST_BODY", "Invalid request body", request, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParam(MissingServletRequestParameterException ex, HttpServletRequest request) {
        return buildErrorResponse("MISSING_PARAMETER", "Missing parameter: " + ex.getParameterName(), request, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        return buildErrorResponse("INVALID_PARAMETER_TYPE", "Invalid type for parameter '" + ex.getName() + "'", request, HttpStatus.BAD_REQUEST);
    }

    // ============ 405 METHOD NOT ALLOWED ============
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        return buildErrorResponse("METHOD_NOT_ALLOWED", ex.getMessage(), request, HttpStatus.METHOD_NOT_ALLOWED);
    }

    // ============ 500 IMAGE UPLOAD ============
    @ExceptionHandler(ImageUploadException.class)
    public ResponseEntity<ErrorResponse> handleImageUpload(ImageUploadException ex, HttpServletRequest request) {
        log.error("Image upload failed at {}: {}", request.getRequestURI(), ex.getMessage(), ex);
        return buildErrorResponse("IMAGE_UPLOAD_ERROR", "Image upload failed. Please try again.", request, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // ============ 500 FALLBACK ============
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleOther(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception at {}: {}", request.getRequestURI(), ex.getMessage(), ex);
        return buildErrorResponse("INTERNAL_SERVER_ERROR", "An internal error occurred. Please try again.", request, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private ResponseEntity<ErrorResponse> buildErrorResponse(String code, String message, HttpServletRequest request, HttpStatus status) {
        ErrorResponse error = ErrorResponse.builder()
                .success(false)
                .code(code)
                .message(message)
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .build();
        return new ResponseEntity<>(error, status);
    }
}