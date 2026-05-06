package com.lakeserl.product_service.exception;

public class ProductVariantLimitException extends RuntimeException {
    public ProductVariantLimitException(String message) {
        super(message);
    }
}
