package com.lakeserl.product_service.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class ProductStatsDTO {
    private Long totalProducts;
    private Long activeProducts;
    private Long outOfStockProducts;
    private Map<String, Long> productsByCategory;
    private Map<String, Long> productsByBrand;
}
