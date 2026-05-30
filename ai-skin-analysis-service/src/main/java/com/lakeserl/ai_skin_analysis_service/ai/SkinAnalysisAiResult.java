package com.lakeserl.ai_skin_analysis_service.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkinAnalysisAiResult {

    private String detectedSkinType;
    private List<String> concerns;
    private Map<String, String> zones;
    private String severity;
    private String advice;
    private List<String> lifestyleNotes;
    private List<String> morningSteps;
    private List<String> eveningSteps;
    private Integer tokensInput;
    private Integer tokensOutput;
    private Long responseTimeMs;
}
