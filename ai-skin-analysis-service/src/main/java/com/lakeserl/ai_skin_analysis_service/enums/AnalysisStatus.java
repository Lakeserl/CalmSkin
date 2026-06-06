package com.lakeserl.ai_skin_analysis_service.enums;

public enum AnalysisStatus {
    PROCESSING,
    COMPLETED,
    /** AI call/parse failed; the user still received a basic fallback result. */
    COMPLETED_DEGRADED,
    FAILED
}
