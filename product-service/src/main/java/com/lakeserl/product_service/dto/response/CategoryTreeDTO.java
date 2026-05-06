package com.lakeserl.product_service.dto.response;

import lombok.Data;

import java.util.List;

@Data
public class CategoryTreeDTO {
    private Long id;
    private String name;
    private String slug;
    private String imageUrl;
    private Integer displayOrder;
    private Boolean isActive;
    private List<CategoryTreeDTO> subCategories;
}
