package com.lakeserl.product_service.dto.response;

import com.lakeserl.product_service.enums.ProductStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ProductSummaryDTO {
    private Long id;
    private String name;
    private String slug;
    private String shortDescription;
    
    private String categoryName;
    private String brandName;
    
    private BigDecimal price; // Computed price (variant or base/sale)
    private BigDecimal originalPrice; // If on sale, the original base price
    private BigDecimal discountPercent;
    
    private String primaryImageUrl;
    
    private Boolean isNewArrival;
    private Boolean isFeatured;
    private ProductStatus status;
    
    private List<String> tags;
    
    private BigDecimal averageRating;
    private Integer totalReviews;
    private Long soldCount;
}
