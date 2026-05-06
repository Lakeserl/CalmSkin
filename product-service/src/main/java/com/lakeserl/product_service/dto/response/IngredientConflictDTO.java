package com.lakeserl.product_service.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class IngredientConflictDTO {
    private String ingredientA;
    private String ingredientB;
    private String severity;
    private String reason;
}
