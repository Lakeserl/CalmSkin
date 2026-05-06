package com.lakeserl.product_service.dto.request;

import com.lakeserl.product_service.enums.ProductUsageStep;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductFilterRequest {
    private String q;
    private String category;
    private String brand;
    private String skinType;
    private String skinConcern;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private String tag;
    private ProductUsageStep usageStep;
    private Boolean isFeatured;
    private Boolean isNew;
    private String sort;
}
