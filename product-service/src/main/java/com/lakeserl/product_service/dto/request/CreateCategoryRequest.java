package com.lakeserl.product_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateCategoryRequest {
    @NotBlank(message = "Category name is required")
    private String name;

    private String description;

    private String imageUrl;

    private Long parentId;

    private Integer displayOrder = 0;

    private Boolean isActive = true;
}
