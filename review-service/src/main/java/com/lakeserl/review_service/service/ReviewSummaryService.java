package com.lakeserl.review_service.service;

import com.lakeserl.review_service.dto.response.ReviewSummaryDTO;
import org.springframework.scheduling.annotation.Async;

public interface ReviewSummaryService {
    ReviewSummaryDTO getSummary(Long productId);

    @Async
    void rebuildSummary(Long productId);
}
