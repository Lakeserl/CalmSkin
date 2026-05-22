package com.lakeserl.promotion_service.support;

import java.util.ArrayList;
import java.util.List;

import com.lakeserl.promotion_service.dto.response.PromotionResponse;
import com.lakeserl.promotion_service.dto.response.PromotionSummaryResponse;
import com.lakeserl.promotion_service.entity.Promotion;

/** Maps {@link Promotion} entities to their API response shapes. */
public final class PromotionMapper {

    private PromotionMapper() {
    }

    public static PromotionResponse toResponse(Promotion p) {
        return new PromotionResponse(
                p.getId(),
                p.getCode(),
                p.getName(),
                p.getDescription(),
                p.getType().name(),
                p.getDiscountType().name(),
                p.getDiscountValue(),
                p.getMaxDiscountAmount(),
                p.getMinOrderValue(),
                p.getMinItemQuantity(),
                new ArrayList<>(CsvIds.parse(p.getApplicableProductIds())),
                new ArrayList<>(CsvIds.parse(p.getApplicableCategoryIds())),
                new ArrayList<>(CsvIds.parse(p.getApplicableBrandIds())),
                new ArrayList<>(CsvIds.parse(p.getExcludedProductIds())),
                p.getTotalUsageLimit(),
                p.getPerUserLimit(),
                p.getStartsAt(),
                p.getEndsAt(),
                p.getStatus().name(),
                Boolean.TRUE.equals(p.getIsStackable()),
                p.getPriority(),
                p.getCreatedBy(),
                p.getCreatedAt(),
                p.getUpdatedAt());
    }

    public static PromotionSummaryResponse toSummary(Promotion p) {
        return new PromotionSummaryResponse(
                p.getId(),
                p.getName(),
                p.getDescription(),
                p.getType().name(),
                p.getDiscountType().name(),
                p.getDiscountValue(),
                p.getMinOrderValue(),
                p.getStartsAt(),
                p.getEndsAt());
    }

    public static List<PromotionSummaryResponse> toSummaries(List<Promotion> promotions) {
        return promotions.stream().map(PromotionMapper::toSummary).toList();
    }
}
