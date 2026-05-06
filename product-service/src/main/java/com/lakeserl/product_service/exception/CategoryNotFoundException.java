package com.lakeserl.product_service.exception;

public class CategoryNotFoundException extends RuntimeException {
    public CategoryNotFoundException(String message) {
        super(message);
    }

    public CategoryNotFoundException(Long id) {
        super("Category not found with id: " + id);
    }
}
