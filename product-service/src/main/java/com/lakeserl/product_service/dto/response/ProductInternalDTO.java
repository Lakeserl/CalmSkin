package com.lakeserl.product_service.dto.response;

import java.math.BigDecimal;

import com.lakeserl.product_service.enums.ProductStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductInternalDTO {
    private Long id;
    private String name;
    private String slug;
    private String sku;
    private ProductStatus status;
    private BigDecimal basePrice;
    private BigDecimal salePrice;
    private Long brandId;
    private Long categoryId;
}
