package com.lakeserl.search_service.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
public class ProductIndexDTO {
    private Long id;
    private String name;
    private String sku;
    private String description;
    private String brandName;
    private String categoryName;
    private List<String> ingredients;
    private List<String> skinTypes;
    private List<String> skinConcerns;
    private BigDecimal price;
    private String status;
    private Long soldCount;
    private Instant createdAt;
    private String primaryImageUrl;
}
