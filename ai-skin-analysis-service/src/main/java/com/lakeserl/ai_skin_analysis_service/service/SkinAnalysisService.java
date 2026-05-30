package com.lakeserl.ai_skin_analysis_service.service;

import com.lakeserl.ai_skin_analysis_service.dto.response.SkinAnalysisResultDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

public interface SkinAnalysisService {

    Map<String, String> initiateAnalysis(MultipartFile image, Integer age, String selfSkinType,
                                          String selfConcerns, String allergies,
                                          boolean consentGiven, Long userId);

    SkinAnalysisResultDTO getSession(String sessionId, Long userId, boolean isAdmin);

    Page<SkinAnalysisResultDTO> getUserHistory(Long userId, Pageable pageable);

    Page<SkinAnalysisResultDTO> getAllSessions(Pageable pageable);

    void deleteSession(String sessionId, Long userId);

    Map<String, Object> getAdminStats();

    SkinAnalysisResultDTO getLatestForUser(Long userId);
}
