package com.lakeserl.ai_skin_analysis_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkinConditionDTO {

    private String zone;
    private String description;
    private String severity;
}
