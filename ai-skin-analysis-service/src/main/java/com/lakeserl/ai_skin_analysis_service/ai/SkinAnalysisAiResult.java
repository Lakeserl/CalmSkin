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

    /** True when this is a fallback because the AI call/parse failed (not a genuine analysis). */
    private boolean degraded;

    /**
     * Fallback used when the Gemini call or parsing fails. Marked degraded so the processor
     * records the session as COMPLETED_DEGRADED and does not propagate it downstream.
     */
    public static SkinAnalysisAiResult degradedFallback(long elapsed) {
        return SkinAnalysisAiResult.builder()
                .detectedSkinType("NORMAL")
                .concerns(List.of())
                .morningSteps(List.of("CLEANSE", "MOISTURIZE", "SPF"))
                .eveningSteps(List.of("CLEANSE", "MOISTURIZE"))
                .advice("Analysis completed with limited results.")
                .tokensInput(0)
                .tokensOutput(0)
                .responseTimeMs(elapsed)
                .degraded(true)
                .build();
    }
}
