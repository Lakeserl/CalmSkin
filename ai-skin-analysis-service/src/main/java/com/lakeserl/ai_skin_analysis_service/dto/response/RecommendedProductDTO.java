package com.lakeserl.ai_skin_analysis_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecommendedProductDTO {

    private Long productId;
    private String name;
    private String imageUrl;
    private String category;
    private BigDecimal price;
}
