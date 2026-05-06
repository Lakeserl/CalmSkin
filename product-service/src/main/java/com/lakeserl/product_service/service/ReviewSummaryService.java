package com.lakeserl.product_service.service;

import com.lakeserl.product_service.dto.request.UpdateReviewSummaryRequest;

public interface ReviewSummaryService {
    void handleReviewSummaryUpdate(UpdateReviewSummaryRequest request);
}
