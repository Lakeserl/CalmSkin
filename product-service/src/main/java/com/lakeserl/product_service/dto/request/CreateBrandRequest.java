package com.lakeserl.product_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateBrandRequest {
    @NotBlank(message = "Brand name is required")
    private String name;

    private String description;

    private String logoUrl;

    private String originCountry;

    private String websiteUrl;

    private Boolean isActive = true;
}
