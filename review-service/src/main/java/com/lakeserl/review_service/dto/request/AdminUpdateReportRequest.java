package com.lakeserl.review_service.dto.request;

import com.lakeserl.review_service.enums.ReportStatus;
import jakarta.validation.constraints.NotNull;

public record AdminUpdateReportRequest(
        @NotNull ReportStatus status
) {}
