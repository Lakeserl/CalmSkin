package com.lakeserl.ai_skin_analysis_service.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * NEW-2: a Gemini outage must degrade (fallback NORMAL, degraded=true) rather than throw and
 * fail the whole session. A genuinely parsed result must NOT be flagged degraded — that's the
 * "AI returned NORMAL" vs "AI failed" distinction the session status relies on.
 */
class GeminiClientImplTest {

    private ChatModel chatModel;
    private GeminiClientImpl client;

    @BeforeEach
    void setUp() {
        chatModel = mock(ChatModel.class);
        PromptBuilder promptBuilder = mock(PromptBuilder.class);
        when(promptBuilder.loadSystemPrompt()).thenReturn("system");
        when(promptBuilder.buildUserPrompt(any(), any(), any(), any())).thenReturn("user");
        client = new GeminiClientImpl(chatModel, promptBuilder, new ObjectMapper());
    }

    @Test
    void geminiApiFailureReturnsDegradedNormalAndDoesNotThrow() {
        when(chatModel.call(any(Prompt.class))).thenThrow(new RuntimeException("503 upstream unavailable"));

        SkinAnalysisAiResult result = client.analyzeImage(new byte[]{1, 2, 3}, "features", 25, "OILY", "ACNE");

        assertThat(result.isDegraded()).isTrue();
        assertThat(result.getDetectedSkinType()).isEqualTo("NORMAL");
    }

    @Test
    void validJsonIsParsedAndNotDegraded() {
        String json = "{\"detectedSkinType\":\"OILY\",\"concerns\":[\"ACNE\"],\"advice\":\"ok\"}";
        when(chatModel.call(any(Prompt.class))).thenReturn(responseWithText(json));

        SkinAnalysisAiResult result = client.analyzeImage(new byte[]{1}, "f", 30, "OILY", "ACNE");

        assertThat(result.isDegraded()).isFalse();
        assertThat(result.getDetectedSkinType()).isEqualTo("OILY");
        assertThat(result.getConcerns()).containsExactly("ACNE");
    }

    @Test
    void malformedContentFallsBackToNormalDegraded() {
        when(chatModel.call(any(Prompt.class))).thenReturn(responseWithText("not json at all"));

        SkinAnalysisAiResult result = client.analyzeImage(new byte[]{1}, "f", 30, null, null);

        assertThat(result.isDegraded()).isTrue();
        assertThat(result.getDetectedSkinType()).isEqualTo("NORMAL");
    }

    @Test
    void geminiErrorResponseMarksDegradedUnknown() {
        String json = "{\"error\":\"unclear\",\"reason\":\"blurry\"}";
        when(chatModel.call(any(Prompt.class))).thenReturn(responseWithText(json));

        SkinAnalysisAiResult result = client.analyzeImage(new byte[]{1}, "f", 30, null, null);

        assertThat(result.isDegraded()).isTrue();
        // raw pre-normalization output — the processor's normalizer maps UNKNOWN -> NORMAL later
        assertThat(result.getDetectedSkinType()).isEqualTo("UNKNOWN");
    }

    private ChatResponse responseWithText(String text) {
        return new ChatResponse(List.of(new Generation(new AssistantMessage(text))));
    }
}
