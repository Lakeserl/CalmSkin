package com.lakeserl.product_service.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateVariantRequest {
    @NotBlank(message = "Variant name is required")
    private String name;

    @NotBlank(message = "Variant SKU is required")
    private String sku;

    @NotNull(message = "Price is required")
    @Min(value = 1, message = "Price must be positive")
    private BigDecimal price;

    private BigDecimal salePrice;

    @Min(value = 0, message = "Stock quantity cannot be negative")
    private Integer stockQuantity = 0;

    private Boolean isDefault = false;
    
    private Boolean isActive = true;
}
