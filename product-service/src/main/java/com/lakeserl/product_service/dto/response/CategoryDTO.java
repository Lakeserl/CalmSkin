package com.lakeserl.product_service.dto.response;

import lombok.Data;

@Data
public class CategoryDTO {
    private Long id;
    private String name;
    private String slug;
    private String description;
    private String imageUrl;
    private Long parentId;
    private String parentName;
    private Integer displayOrder;
    private Boolean isActive;
}
