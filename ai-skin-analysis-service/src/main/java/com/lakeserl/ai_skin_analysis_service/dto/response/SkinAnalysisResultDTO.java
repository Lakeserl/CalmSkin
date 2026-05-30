package com.lakeserl.ai_skin_analysis_service.dto.response;

import com.lakeserl.ai_skin_analysis_service.enums.AnalysisStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkinAnalysisResultDTO {

    private String sessionId;
    private Long userId;
    private AnalysisStatus status;
    private String detectedSkinType;
    private List<String> detectedConcerns;
    private String skinConditionReport;
    private List<RecommendedProductDTO> recommendedProducts;
    private RoutineDTO morningRoutine;
    private RoutineDTO eveningRoutine;
    private String failureReason;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
}
