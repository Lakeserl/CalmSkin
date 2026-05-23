package com.lakeserl.review_service.dto.request;

import com.lakeserl.review_service.enums.ReportReason;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateReportRequest(
        @NotNull ReportReason reason,
        @Size(max = 1000) String detail
) {}
