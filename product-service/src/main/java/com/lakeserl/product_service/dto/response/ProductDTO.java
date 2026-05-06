package com.lakeserl.product_service.dto.response;

import com.lakeserl.product_service.enums.ProductStatus;
import com.lakeserl.product_service.enums.ProductUsageStep;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ProductDTO {
    private Long id;
    private String name;
    private String slug;
    private String sku;
    private String description;
    private String shortDescription;
    private String howToUse;
    
    private CategoryDTO category;
    private BrandDTO brand;
    
    private BigDecimal basePrice;
    private BigDecimal salePrice;
    private BigDecimal discountPercent;
    
    private ProductUsageStep usageStep;
    private List<String> suitableSkinTypes;
    private List<String> skinConcerns;
    
    private Integer volumeMl;
    private Integer weightG;
    private Integer shelfLifeMonths;
    
    private Boolean isFeatured;
    private Boolean isNewArrival;
    private ProductStatus status;
    
    private Long viewCount;
    private Long soldCount;
    
    private List<ProductVariantDTO> variants;
    private List<ProductImageDTO> images;
    private List<IngredientDTO> keyIngredients;
    private List<String> tags;
    
    private ReviewSummaryDTO reviewSummary;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
