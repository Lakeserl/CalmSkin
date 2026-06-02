package com.lakeserl.search_service.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class SearchResultDTO {
    private String id;
    private String name;
    private String brandName;
    private String categoryName;
    private BigDecimal price;
    private String status;
    private String primaryImageUrl;
    private Long soldCount;
    // Highlighted snippet if query matched inside description/ingredients
    private String highlight;
}
