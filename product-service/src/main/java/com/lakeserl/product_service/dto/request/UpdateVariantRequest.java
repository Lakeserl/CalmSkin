package com.lakeserl.product_service.dto.request;

import jakarta.validation.constraints.Min;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class UpdateVariantRequest {
    private String name;
    
    private String sku;

    @Min(value = 1, message = "Price must be positive")
    private BigDecimal price;

    private BigDecimal salePrice;

    @Min(value = 0, message = "Stock quantity cannot be negative")
    private Integer stockQuantity;

    private Boolean isDefault;
    
    private Boolean isActive;
}
