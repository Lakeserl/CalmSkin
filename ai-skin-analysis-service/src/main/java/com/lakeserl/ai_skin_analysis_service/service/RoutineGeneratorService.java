package com.lakeserl.ai_skin_analysis_service.service;

import com.lakeserl.ai_skin_analysis_service.dto.response.RoutineDTO;

import java.util.List;

public interface RoutineGeneratorService {

    RoutineDTO generateMorningRoutine(String detectedSkinType, List<String> concerns);

    RoutineDTO generateEveningRoutine(String detectedSkinType, List<String> concerns);
}
