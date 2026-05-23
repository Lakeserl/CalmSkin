package com.lakeserl.review_service.dto.response;

import com.lakeserl.review_service.enums.ReportReason;
import com.lakeserl.review_service.enums.ReportStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class ReviewReportDTO {
    private Long id;
    private Long reviewId;
    private UUID reporterId;
    private ReportReason reason;
    private String detail;
    private ReportStatus status;
    private LocalDateTime createdAt;
}

