package com.lakeserl.product_service.exception;

public class ProductImageLimitException extends RuntimeException {
    public ProductImageLimitException(String message) {
        super(message);
    }
}
