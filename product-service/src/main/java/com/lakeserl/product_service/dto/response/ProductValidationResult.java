package com.lakeserl.product_service.dto.response;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductValidationResult {
    private Long productId;
    private Long variantId;
    private String productName;
    private String productSku;
    private String variantName;
    private String productImageUrl;
    private String brandName;
    private BigDecimal unitPrice;
    private boolean valid;
    private String reason;
}

