package com.lakeserl.product_service.exception;

public class ProductNotActiveException extends RuntimeException {
    public ProductNotActiveException(String message) {
        super(message);
    }
    
    public ProductNotActiveException(Long id) {
        super("Product is not active: " + id);
    }
}
