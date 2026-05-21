package com.lakeserl.notification_service.service;

import java.util.Map;

import org.springframework.stereotype.Service;

import com.lakeserl.notification_service.entity.NotificationTemplate;
import com.lakeserl.notification_service.enums.NotificationChannel;
import com.lakeserl.notification_service.repository.NotificationTemplateRepository;

import lombok.RequiredArgsConstructor;

/**
 * Resolves the subject/body text for a notification. A DB template (matched by
 * code + channel + locale) wins; if none exists the fallback title/body carried
 * on the command is used, so a missing template never silently drops a message.
 * Placeholders use the {@code {{name}}} syntax.
 */
@Service
@RequiredArgsConstructor
public class TemplateService {

    private final NotificationTemplateRepository templateRepository;

    /** Rendered, ready-to-deliver text for one channel. */
    public record RenderedContent(String subject, String body) {
    }

    public RenderedContent render(String templateCode,
                                  NotificationChannel channel,
                                  String locale,
                                  Map<String, String> variables,
                                  String fallbackSubject,
                                  String fallbackBody) {

        NotificationTemplate template = templateRepository
                .findFirstByCodeAndChannelAndLocaleAndIsActiveTrue(templateCode, channel, locale)
                .or(() -> templateRepository.findFirstByCodeAndChannelAndIsActiveTrue(templateCode, channel))
                .orElse(null);

        String subject = template != null && template.getSubject() != null
                ? template.getSubject() : fallbackSubject;
        String body = template != null ? template.getBody() : fallbackBody;

        return new RenderedContent(
                substitute(subject, variables),
                substitute(body, variables));
    }

    private String substitute(String text, Map<String, String> variables) {
        if (text == null || variables == null || variables.isEmpty()) {
            return text;
        }
        String result = text;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            String value = entry.getValue() == null ? "" : entry.getValue();
            result = result.replace("{{" + entry.getKey() + "}}", value);
        }
        return result;
    }
}
