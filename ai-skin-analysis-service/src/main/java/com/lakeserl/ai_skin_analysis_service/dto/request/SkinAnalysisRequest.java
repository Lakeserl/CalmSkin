package com.lakeserl.ai_skin_analysis_service.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkinAnalysisRequest {

    private Integer age;
    private String selfSkinType;
    private String selfConcerns;
    private String allergies;
}
