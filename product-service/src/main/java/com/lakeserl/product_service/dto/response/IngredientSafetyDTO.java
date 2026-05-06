package com.lakeserl.product_service.dto.response;

import com.lakeserl.product_service.enums.IngredientSafetyLevel;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class IngredientSafetyDTO {
    private String ingredientName;
    private IngredientSafetyLevel status; // SAFE, CAUTION, AVOID
    private String reason;
}
