package com.lakeserl.ai_skin_analysis_service.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
public class PromptBuilder {

    public String loadSystemPrompt() {
        return loadResource("prompts/skin-analysis-system.txt");
    }

    public String buildUserPrompt(String cvFeatures, Integer age, String selfSkinType, String concerns) {
        String template = loadResource("prompts/skin-analysis-user.txt");
        return template
                .replace("{age}", age != null ? String.valueOf(age) : "not specified")
                .replace("{selfSkinType}", selfSkinType != null ? selfSkinType : "not specified")
                .replace("{concerns}", concerns != null ? concerns : "none reported")
                .replace("{cvFeatures}", cvFeatures != null ? cvFeatures : "{}");
    }

    private String loadResource(String path) {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(path)) {
            if (in == null) {
                log.warn("Prompt resource not found: {}", path);
                return "";
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("Failed to load prompt resource {}: {}", path, e.getMessage());
            return "";
        }
    }
}
