package com.lakeserl.order_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemDTO {
    private Long id;
    private Long productId;
    private Long variantId;
    private String productName;
    private String productSku;
    private String variantName;
    private String productImageUrl;
    private String brandName;
    private BigDecimal unitPrice;
    private Integer quantity;
    private BigDecimal subtotal;
}
