package com.lakeserl.ai_skin_analysis_service.ai;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;
import org.springframework.ai.content.Media;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeTypeUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class GeminiClientImpl implements GeminiClient {

    private final ChatModel chatModel;
    private final PromptBuilder promptBuilder;
    private final ObjectMapper objectMapper;

    // Circuit breaker fast-fails (and counts slow calls as failures) when Gemini is
    // down or degraded, so a backlog of 20s+ vision calls cannot exhaust the @Async
    // pool. Real failures and CircuitBreaker-open both route to analyzeImageFallback.
    @Override
    @CircuitBreaker(name = "gemini-vision", fallbackMethod = "analyzeImageFallback")
    public SkinAnalysisAiResult analyzeImage(byte[] imageBytes, String features,
                                              Integer age, String selfSkinType,
                                              String concerns) {
        String systemPromptText = promptBuilder.loadSystemPrompt();
        String userPromptText = promptBuilder.buildUserPrompt(features, age, selfSkinType, concerns);

        Media media = new Media(MimeTypeUtils.IMAGE_JPEG, new ByteArrayResource(imageBytes));
        UserMessage userMessage = UserMessage.builder().text(userPromptText).media(media).build();
        SystemMessage systemMessage = new SystemMessage(systemPromptText);

        GoogleGenAiChatOptions structuredOptions = GoogleGenAiChatOptions.builder()
                .responseMimeType("application/json")
                .build();

        Prompt prompt = new Prompt(List.of(systemMessage, userMessage), structuredOptions);

        long startTime = System.currentTimeMillis();
        ChatResponse response = chatModel.call(prompt);
        long elapsed = System.currentTimeMillis() - startTime;

        String content = response.getResult().getOutput().getText();
        log.info("Gemini response received in {}ms, content length={}", elapsed, content.length());

        return parseResponse(content, elapsed, response);
    }

    /** Resilience4j fallback: Gemini outage / 4xx / timeout / circuit open. */
    @SuppressWarnings("unused")
    private SkinAnalysisAiResult analyzeImageFallback(byte[] imageBytes, String features,
                                                      Integer age, String selfSkinType,
                                                      String concerns, Throwable t) {
        log.error("Gemini vision unavailable ({}) — returning degraded fallback", t.toString());
        return SkinAnalysisAiResult.degradedFallback(0);
    }

    @SuppressWarnings("unchecked")
    private SkinAnalysisAiResult parseResponse(String content, long elapsed, ChatResponse response) {
        try {
            String json = extractJson(content);
            Map<String, Object> parsed = objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});

            if (parsed.containsKey("error")) {
                log.warn("Gemini returned error: {}", parsed.get("error"));
                return SkinAnalysisAiResult.builder()
                        .detectedSkinType("UNKNOWN")
                        .concerns(List.of())
                        .morningSteps(List.of())
                        .eveningSteps(List.of())
                        .advice(String.valueOf(parsed.getOrDefault("reason", "Image analysis failed")))
                        .tokensInput(0)
                        .tokensOutput(0)
                        .responseTimeMs(elapsed)
                        .degraded(true)
                        .build();
            }

            String skinType = (String) parsed.getOrDefault("detectedSkinType", "NORMAL");
            List<String> concerns = castList(parsed.get("concerns"));
            Map<String, String> zones = castStringMap(parsed.get("zones"));
            String severity = (String) parsed.getOrDefault("severity", "MILD");
            String advice = (String) parsed.getOrDefault("advice", "");
            List<String> lifestyleNotes = castList(parsed.get("lifestyleNotes"));

            Map<String, Object> recommendations = (Map<String, Object>) parsed.getOrDefault("recommendations", Map.of());
            List<String> morningSteps = castList(recommendations.get("morningSteps"));
            List<String> eveningSteps = castList(recommendations.get("eveningSteps"));

            int tokensInput = extractTokensInput(response);
            int tokensOutput = extractTokensOutput(response);

            return SkinAnalysisAiResult.builder()
                    .detectedSkinType(skinType)
                    .concerns(concerns)
                    .zones(zones)
                    .severity(severity)
                    .advice(advice)
                    .lifestyleNotes(lifestyleNotes)
                    .morningSteps(morningSteps)
                    .eveningSteps(eveningSteps)
                    .tokensInput(tokensInput)
                    .tokensOutput(tokensOutput)
                    .responseTimeMs(elapsed)
                    .build();

        } catch (Exception e) {
            log.error("Failed to parse Gemini response: {} — returning degraded fallback", e.getMessage());
            return SkinAnalysisAiResult.degradedFallback(elapsed);
        }
    }

    private String extractJson(String content) {
        if (content == null || content.isBlank()) {
            return "{}";
        }
        int start = content.indexOf('{');
        int end = content.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return content.substring(start, end + 1);
        }
        return content.trim();
    }

    @SuppressWarnings("unchecked")
    private List<String> castList(Object obj) {
        if (obj instanceof List<?> list) {
            List<String> result = new ArrayList<>();
            for (Object item : list) {
                if (item != null) {
                    result.add(String.valueOf(item));
                }
            }
            return result;
        }
        return new ArrayList<>();
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> castStringMap(Object obj) {
        if (obj instanceof Map<?, ?> map) {
            java.util.LinkedHashMap<String, String> result = new java.util.LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                result.put(String.valueOf(entry.getKey()), String.valueOf(entry.getValue()));
            }
            return result;
        }
        return Map.of();
    }

    private int extractTokensInput(ChatResponse response) {
        try {
            if (response.getMetadata() != null && response.getMetadata().getUsage() != null) {
                Integer promptTokens = response.getMetadata().getUsage().getPromptTokens();
                return promptTokens != null ? promptTokens.intValue() : 0;
            }
        } catch (Exception e) {
            log.debug("Could not extract input tokens: {}", e.getMessage());
        }
        return 0;
    }

    private int extractTokensOutput(ChatResponse response) {
        try {
            if (response.getMetadata() != null && response.getMetadata().getUsage() != null) {
                Integer completionTokens = response.getMetadata().getUsage().getCompletionTokens();
                return completionTokens != null ? completionTokens.intValue() : 0;
            }
        } catch (Exception e) {
            log.debug("Could not extract output tokens: {}", e.getMessage());
        }
        return 0;
    }
}
