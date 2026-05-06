package com.lakeserl.product_service.exception;

public class DuplicateSkuException extends RuntimeException {
    public DuplicateSkuException(String sku) {
        super("SKU already exists: " + sku);
    }
}
