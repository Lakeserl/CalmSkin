package com.lakeserl.ai_skin_analysis_service.controller;

import com.lakeserl.ai_skin_analysis_service.dto.response.ApiResponse;
import com.lakeserl.ai_skin_analysis_service.dto.response.SkinAnalysisResultDTO;
import com.lakeserl.ai_skin_analysis_service.service.SkinAnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/ai/skin-analysis")
@RequiredArgsConstructor
@Tag(name = "Admin — Skin Analysis", description = "Admin-only skin analysis management endpoints")
public class AdminSkinAnalysisController {

    private final SkinAnalysisService skinAnalysisService;

    @GetMapping("/stats")
    @Operation(summary = "Overall usage statistics (token usage, costs, session counts)")
    public ApiResponse<Map<String, Object>> getStats() {
        return ApiResponse.ok(skinAnalysisService.getAdminStats());
    }

    @GetMapping("/sessions")
    @Operation(summary = "All sessions paginated")
    public ApiResponse<Page<SkinAnalysisResultDTO>> getAllSessions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        return ApiResponse.ok(skinAnalysisService.getAllSessions(
                PageRequest.of(page, size, Sort.by("createdAt").descending())));
    }
}
