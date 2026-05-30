package com.lakeserl.ai_skin_analysis_service.event.payload;

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
public class SkinAnalysisCompletedEvent {

    private String sessionId;
    private Long userId;
    private String detectedSkinType;
    private List<String> concerns;
    private LocalDateTime completedAt;
}
