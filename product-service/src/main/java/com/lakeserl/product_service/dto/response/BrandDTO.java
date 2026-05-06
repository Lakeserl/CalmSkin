package com.lakeserl.product_service.dto.response;

import lombok.Data;

@Data
public class BrandDTO {
    private Long id;
    private String name;
    private String slug;
    private String description;
    private String logoUrl;
    private String originCountry;
    private String websiteUrl;
    private Boolean isActive;
}
