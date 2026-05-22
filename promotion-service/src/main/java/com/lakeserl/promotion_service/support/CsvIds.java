package com.lakeserl.promotion_service.support;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Encodes/decodes the comma-separated id lists stored in the promotion scope
 * columns ({@code applicable_product_ids}, etc.).
 */
public final class CsvIds {

    private CsvIds() {
    }

    /** Parses "1,2,3" into a set of ids; blank/null yields an empty set. */
    public static Set<Long> parse(String csv) {
        Set<Long> out = new LinkedHashSet<>();
        if (csv == null || csv.isBlank()) {
            return out;
        }
        for (String part : csv.split(",")) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                try {
                    out.add(Long.parseLong(trimmed));
                } catch (NumberFormatException ignored) {
                    // skip malformed id
                }
            }
        }
        return out;
    }

    /** Joins ids into "1,2,3"; null/empty yields null (column stays NULL). */
    public static String join(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return null;
        }
        String joined = ids.stream()
                .filter(Objects::nonNull)
                .map(String::valueOf)
                .collect(Collectors.joining(","));
        return joined.isEmpty() ? null : joined;
    }
}
