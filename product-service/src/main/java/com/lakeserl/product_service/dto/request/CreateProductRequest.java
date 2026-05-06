package com.lakeserl.product_service.dto.request;

import com.lakeserl.product_service.enums.ProductUsageStep;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class CreateProductRequest {
    @NotBlank(message = "Product name is required")
    @Size(min = 2, max = 200, message = "Name must be between 2 and 200 characters")
    private String name;

    @NotBlank(message = "SKU is required")
    private String sku;

    @Size(max = 5000, message = "Description must be at most 5000 characters")
    private String description;

    @Size(max = 500, message = "Short description must be at most 500 characters")
    private String shortDescription;

    private String howToUse;

    @NotNull(message = "Category ID is required")
    private Long categoryId;

    @NotNull(message = "Brand ID is required")
    private Long brandId;

    @NotNull(message = "Base price is required")
    @Min(value = 1, message = "Base price must be greater than 0")
    private BigDecimal basePrice;

    private BigDecimal salePrice;

    @NotNull(message = "Usage step is required")
    private ProductUsageStep usageStep;

    private List<String> suitableSkinTypes;
    
    private List<String> skinConcerns;

    @Min(value = 1, message = "Volume must be positive")
    private Integer volumeMl;

    @Min(value = 1, message = "Weight must be positive")
    private Integer weightG;

    @Min(value = 1, message = "Shelf life must be positive")
    private Integer shelfLifeMonths;

    private Boolean isFeatured = false;
    
    private Boolean isNewArrival = false;
}
