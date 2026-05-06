package com.lakeserl.product_service.exception;

public class TagNotFoundException extends RuntimeException {
    public TagNotFoundException(String message) {
        super(message);
    }

    public TagNotFoundException(Long id) {
        super("Tag not found with id: " + id);
    }
}
