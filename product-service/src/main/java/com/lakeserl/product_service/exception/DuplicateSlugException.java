package com.lakeserl.product_service.exception;

public class DuplicateSlugException extends RuntimeException {
    public DuplicateSlugException(String slug) {
        super("Slug already exists: " + slug);
    }
}
