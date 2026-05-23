package com.lakeserl.review_service.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdminReviewStatsDTO {
    private long totalReviews;
    private long publishedCount;
    private long pendingModerationCount;
    private long hiddenCount;
    private long deletedCount;
    private long pendingReportsCount;
}
