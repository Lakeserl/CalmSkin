package com.lakeserl.review_service.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class ReviewSummaryDTO {
    private Long productId;
    private int totalCount;
    private BigDecimal averageRating;
    private int count1star;
    private int count2star;
    private int count3star;
    private int count4star;
    private int count5star;
    private int countOily;
    private int countDry;
    private int countCombination;
    private int countSensitive;
    private int countNormal;
    private BigDecimal avgSkinEffect;
    private BigDecimal avgTexture;
    private BigDecimal avgScent;
    private BigDecimal avgPackaging;
    private BigDecimal avgValue;
}
