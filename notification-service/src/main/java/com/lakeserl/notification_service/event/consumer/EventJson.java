package com.lakeserl.notification_service.event.consumer;

import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;

/** Null-safe helpers for reading fields out of loosely-typed Kafka event JSON. */
final class EventJson {

    private EventJson() {
    }

    /** Returns the trimmed text of a field, or null when missing/blank. */
    static String text(JsonNode node, String field) {
        String value = node.path(field).asText(null);
        return (value == null || value.isBlank()) ? null : value.trim();
    }

    /** Returns the text of the first present field, or {@code fallback}. */
    static String textOr(JsonNode node, String fallback, String... fields) {
        for (String field : fields) {
            String value = text(node, field);
            if (value != null) {
                return value;
            }
        }
        return fallback;
    }

    /** Parses a field as a UUID, or null when missing/invalid. */
    static UUID uuid(JsonNode node, String field) {
        String value = text(node, field);
        if (value == null) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
