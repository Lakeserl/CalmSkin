package com.lakeserl.product_service.dto.request;

import lombok.Data;

@Data
public class UpdateBrandRequest {
    private String name;
    private String description;
    private String logoUrl;
    private String originCountry;
    private String websiteUrl;
    private Boolean isActive;
}
