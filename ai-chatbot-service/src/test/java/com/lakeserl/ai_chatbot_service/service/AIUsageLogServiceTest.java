package com.lakeserl.ai_chatbot_service.service;

import com.lakeserl.ai_chatbot_service.entity.AIUsageLog;
import com.lakeserl.ai_chatbot_service.repository.AIUsageLogRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Every Gemini call must leave a usage record (tokens + model + outcome) for cost monitoring,
 * including failures — so daily AI cost is auditable across services.
 */
class AIUsageLogServiceTest {

    private final AIUsageLogRepository repository = mock(AIUsageLogRepository.class);
    private final AIUsageLogService service = new AIUsageLogService(repository);

    @Test
    void persistsUsageWithProvidedFields() {
        UUID userId = UUID.randomUUID();

        service.record(userId, "gemini-1.5-flash", 30, 70, 250, true, null);

        ArgumentCaptor<AIUsageLog> captor = ArgumentCaptor.forClass(AIUsageLog.class);
        verify(repository).save(captor.capture());
        AIUsageLog saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(userId);
        assertThat(saved.getServiceName()).isEqualTo("ai-chatbot-service");
        assertThat(saved.getModelName()).isEqualTo("gemini-1.5-flash");
        assertThat(saved.getTokensInput()).isEqualTo(30);
        assertThat(saved.getTokensOutput()).isEqualTo(70);
        assertThat(saved.getResponseTimeMs()).isEqualTo(250);
        assertThat(saved.getSuccess()).isTrue();
    }

    @Test
    void recordsFailureWithErrorMessage() {
        service.record(UUID.randomUUID(), "gemini-1.5-flash", 0, 0, 100, false, "broker down");

        ArgumentCaptor<AIUsageLog> captor = ArgumentCaptor.forClass(AIUsageLog.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getSuccess()).isFalse();
        assertThat(captor.getValue().getErrorMessage()).isEqualTo("broker down");
    }
}
