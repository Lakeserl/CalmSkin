package com.lakeserl.ai_skin_analysis_service.controller;

import com.lakeserl.ai_skin_analysis_service.dto.response.ApiResponse;
import com.lakeserl.ai_skin_analysis_service.dto.response.SkinAnalysisResultDTO;
import com.lakeserl.ai_skin_analysis_service.service.SkinAnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/ai/skin-analysis")
@RequiredArgsConstructor
@Tag(name = "Internal — Skin Analysis", description = "Service-to-service endpoints (requires X-Internal-Secret header)")
public class InternalController {

    private final SkinAnalysisService skinAnalysisService;

    @GetMapping("/{sessionId}")
    @Operation(summary = "Fetch a single session by ID (for user-service)")
    public ApiResponse<SkinAnalysisResultDTO> getSession(@PathVariable String sessionId) {
        SkinAnalysisResultDTO result = skinAnalysisService.getSession(sessionId, null, true);
        return ApiResponse.ok(result);
    }

    @GetMapping("/user/{userId}/latest")
    @Operation(summary = "Fetch the most recent completed session for a user")
    public ApiResponse<SkinAnalysisResultDTO> getLatestForUser(@PathVariable Long userId) {
        SkinAnalysisResultDTO result = skinAnalysisService.getLatestForUser(userId);
        return ApiResponse.ok(result);
    }
}
