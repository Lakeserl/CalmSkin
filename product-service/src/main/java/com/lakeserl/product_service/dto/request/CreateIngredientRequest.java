package com.lakeserl.product_service.dto.request;

import com.lakeserl.product_service.enums.IngredientSafetyLevel;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class CreateIngredientRequest {
    @NotBlank(message = "Ingredient name is required")
    private String name;

    private String inciName;
    private String description;
    private List<String> benefits;
    private String sideEffects;
    private IngredientSafetyLevel safetyLevel = IngredientSafetyLevel.SAFE;
    private List<String> suitableSkinTypes;
    private List<String> avoidSkinConcerns;
    private Boolean isCommonAllergen = false;
}
