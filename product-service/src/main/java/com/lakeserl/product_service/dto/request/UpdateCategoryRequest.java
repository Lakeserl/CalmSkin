package com.lakeserl.product_service.dto.request;

import lombok.Data;

@Data
public class UpdateCategoryRequest {
    private String name;
    private String description;
    private String imageUrl;
    private Long parentId;
    private Integer displayOrder;
    private Boolean isActive;
}
