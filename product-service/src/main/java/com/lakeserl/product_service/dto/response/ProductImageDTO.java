package com.lakeserl.product_service.dto.response;

import lombok.Data;

@Data
public class ProductImageDTO {
    private Long id;
    private String url;
    private String altText;
    private Integer displayOrder;
    private Boolean isPrimary;
}
