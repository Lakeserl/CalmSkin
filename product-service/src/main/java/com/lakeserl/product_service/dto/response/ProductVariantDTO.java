package com.lakeserl.product_service.dto.response;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductVariantDTO {
    private Long id;
    private String name;
    private String sku;
    private BigDecimal price;
    private BigDecimal salePrice;
    private BigDecimal discountPercent;
    private Integer stockQuantity;
    private Boolean isDefault;
    private Boolean isActive;
}
