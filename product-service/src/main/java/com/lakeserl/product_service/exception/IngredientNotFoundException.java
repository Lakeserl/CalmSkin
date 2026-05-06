package com.lakeserl.product_service.exception;

public class IngredientNotFoundException extends RuntimeException {
    public IngredientNotFoundException(String message) {
        super(message);
    }

    public IngredientNotFoundException(Long id) {
        super("Ingredient not found with id: " + id);
    }
}
