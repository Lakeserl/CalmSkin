package com.lakeserl.ai_skin_analysis_service.controller;

import com.lakeserl.ai_skin_analysis_service.dto.response.ApiResponse;
import com.lakeserl.ai_skin_analysis_service.dto.response.SkinAnalysisResultDTO;
import com.lakeserl.ai_skin_analysis_service.service.SkinAnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/ai/skin-analysis")
@RequiredArgsConstructor
@Tag(name = "Skin Analysis", description = "AI-powered skin analysis from face photo")
public class SkinAnalysisController {

    private final SkinAnalysisService skinAnalysisService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(summary = "Submit a face photo for AI skin analysis (async — returns sessionId)",
               description = "consentGiven=true is required: your face photo is biometric data "
                             + "processed by Google Gemini API (outside Vietnam) and stored on Cloudinary (Decree 13/2023).")
    public ApiResponse<Map<String, String>> startAnalysis(
            @RequestParam("image") MultipartFile image,
            @RequestParam(required = false) Integer age,
            @RequestParam(required = false) String selfSkinType,
            @RequestParam(required = false) String selfConcerns,
            @RequestParam(required = false) String allergies,
            @RequestParam(defaultValue = "false") boolean consentGiven,
            Authentication authentication) {

        Long userId = parseUserId(authentication);
        Map<String, String> result = skinAnalysisService.initiateAnalysis(
                image, age, selfSkinType, selfConcerns, allergies, consentGiven, userId);
        return ApiResponse.ok(result);
    }

    @GetMapping("/{sessionId}")
    @Operation(summary = "Poll for analysis result by sessionId")
    public ApiResponse<SkinAnalysisResultDTO> getResult(
            @PathVariable String sessionId,
            Authentication authentication) {

        Long userId = parseUserId(authentication);
        boolean isAdmin = hasAdminRole(authentication);
        SkinAnalysisResultDTO result = skinAnalysisService.getSession(sessionId, userId, isAdmin);
        return ApiResponse.ok(result);
    }

    @GetMapping("/history")
    @Operation(summary = "Get current user's analysis history (paginated)")
    public ApiResponse<Page<SkinAnalysisResultDTO>> getHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {

        Long userId = parseUserId(authentication);
        Page<SkinAnalysisResultDTO> history = skinAnalysisService.getUserHistory(
                userId, PageRequest.of(page, size, Sort.by("createdAt").descending()));
        return ApiResponse.ok(history);
    }

    @DeleteMapping("/{sessionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete an analysis session")
    public void deleteSession(@PathVariable String sessionId, Authentication authentication) {
        Long userId = parseUserId(authentication);
        skinAnalysisService.deleteSession(sessionId, userId);
    }

    private Long parseUserId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new org.springframework.security.access.AccessDeniedException("Not authenticated");
        }
        try {
            return Long.parseLong(authentication.getName());
        } catch (NumberFormatException e) {
            throw new org.springframework.security.access.AccessDeniedException("Invalid user identity");
        }
    }

    private boolean hasAdminRole(Authentication authentication) {
        if (authentication == null) return false;
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals("ROLE_ADMIN") || a.equals("ROLE_STAFF"));
    }
}
