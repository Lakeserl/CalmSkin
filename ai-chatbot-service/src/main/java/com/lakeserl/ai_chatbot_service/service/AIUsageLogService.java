package com.lakeserl.ai_chatbot_service.service;

import com.lakeserl.ai_chatbot_service.entity.AIUsageLog;
import com.lakeserl.ai_chatbot_service.repository.AIUsageLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Persists one AI-usage record per Gemini call for cost monitoring (uniform with the other AI
 * services). Logging must never break the chat flow, so failures here are swallowed and logged.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AIUsageLogService {

    private static final String SERVICE_NAME = "ai-chatbot-service";

    private final AIUsageLogRepository repository;

    public void record(UUID userId, String modelName, int tokensInput, int tokensOutput,
                       int responseTimeMs, boolean success, String errorMessage) {
        try {
            repository.save(AIUsageLog.builder()
                    .userId(userId)
                    .serviceName(SERVICE_NAME)
                    .modelName(modelName)
                    .tokensInput(tokensInput)
                    .tokensOutput(tokensOutput)
                    .responseTimeMs(responseTimeMs)
                    .success(success)
                    .errorMessage(errorMessage)
                    .build());
        } catch (Exception e) {
            log.warn("Failed to persist AI usage log: {}", e.getMessage());
        }
    }
}
