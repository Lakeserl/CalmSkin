package com.lakeserl.review_service.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class ReviewReplyDTO {
    private Long id;
    private Long reviewId;
    private UUID userId;
    private boolean seller;
    private String body;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

