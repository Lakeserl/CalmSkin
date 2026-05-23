package com.lakeserl.product_service.dto.request;

import lombok.Data;

@Data
public class ProductValidationRequest {
    private Long productId;
    private Long variantId;
    private Integer quantity;
}

