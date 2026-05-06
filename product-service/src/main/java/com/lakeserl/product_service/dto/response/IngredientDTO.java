package com.lakeserl.product_service.dto.response;

import com.lakeserl.product_service.enums.IngredientSafetyLevel;
import lombok.Data;

import java.util.List;

@Data
public class IngredientDTO {
    private Long id;
    private String name;
    private String inciName;
    private String description;
    private List<String> benefits;
    private String sideEffects;
    private IngredientSafetyLevel safetyLevel;
    private List<String> suitableSkinTypes;
    private List<String> avoidSkinConcerns;
    private Boolean isCommonAllergen;
}
