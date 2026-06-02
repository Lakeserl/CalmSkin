package com.lakeserl.ai_recommendation_service.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ProductDTO {
    private Long id;
    private String name;
    private String slug;
    private String shortDescription;
    private String categoryName;
    private String brandName;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private BigDecimal discountPercent;
    private String primaryImageUrl;
    private List<String> skinTypes;
    private List<String> skinConcerns;
    private List<String> tags;
    private BigDecimal averageRating;
    private Integer totalReviews;
    private Long soldCount;
}
