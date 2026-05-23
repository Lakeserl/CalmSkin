package com.lakeserl.review_service.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class EligibilityDTO {
    private Long orderItemId;
    private Long productId;
    private LocalDateTime orderCompletedAt;
    private boolean reviewed;
}
