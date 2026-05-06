package com.lakeserl.product_service.dto.event;

import com.lakeserl.product_service.enums.ProductStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductUpdatedEvent {
    private Long productId;
    private String name;
    private String slug;
    private String primaryImageUrl;
    private BigDecimal basePrice;
    private ProductStatus status;
    
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
}
