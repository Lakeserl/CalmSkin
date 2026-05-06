package com.lakeserl.product_service.exception;

public class BrandNotFoundException extends RuntimeException {
    public BrandNotFoundException(String message) {
        super(message);
    }

    public BrandNotFoundException(Long id) {
        super("Brand not found with id: " + id);
    }
}
