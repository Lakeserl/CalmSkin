package com.lakeserl.product_service.dto.request;

import com.lakeserl.product_service.enums.SkinType;
import lombok.Data;

import java.util.List;

@Data
public class CheckSafeRequest {
    private List<String> ingredientNames;
    private SkinType skinType;
}
