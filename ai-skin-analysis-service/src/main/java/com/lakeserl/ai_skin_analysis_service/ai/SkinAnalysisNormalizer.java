package com.lakeserl.ai_skin_analysis_service.ai;

import com.lakeserl.ai_skin_analysis_service.enums.SkinConcern;
import com.lakeserl.ai_skin_analysis_service.enums.SkinType;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Defensive normalization of free-text AI output into canonical {@link SkinType} /
 * {@link SkinConcern} enum values before anything is persisted or sent downstream.
 *
 * Gemini is not pinned to a responseSchema yet, so it can return Vietnamese labels
 * ("da dầu"), arbitrary casing, or "UNKNOWN". Unmapped skin types fall back to NORMAL;
 * unmapped concerns are dropped. This layer is kept as defense-in-depth even after
 * responseSchema is enabled (Batch 3).
 */
@Component
public class SkinAnalysisNormalizer {

    // Vietnamese label -> enum. Keys are intentionally Vietnamese: they are possible
    // model output values (data), not code identifiers. Lookups are lower-cased first.
    private static final Map<String, SkinType> SKIN_TYPE_VN = Map.of(
            "da khô", SkinType.DRY,
            "da dầu", SkinType.OILY,
            "da nhờn", SkinType.OILY,
            "da hỗn hợp", SkinType.COMBINATION,
            "da nhạy cảm", SkinType.SENSITIVE,
            "da thường", SkinType.NORMAL,
            "da bình thường", SkinType.NORMAL
    );

    private static final Map<String, SkinConcern> CONCERN_VN = Map.ofEntries(
            Map.entry("mụn", SkinConcern.ACNE),
            Map.entry("mụn trứng cá", SkinConcern.ACNE),
            Map.entry("nám", SkinConcern.DARK_SPOTS),
            Map.entry("đốm nâu", SkinConcern.DARK_SPOTS),
            Map.entry("tàn nhang", SkinConcern.DARK_SPOTS),
            Map.entry("nếp nhăn", SkinConcern.WRINKLES),
            Map.entry("lão hóa", SkinConcern.WRINKLES),
            Map.entry("đỏ", SkinConcern.REDNESS),
            Map.entry("ửng đỏ", SkinConcern.REDNESS),
            Map.entry("mẩn đỏ", SkinConcern.REDNESS),
            Map.entry("lỗ chân lông to", SkinConcern.ENLARGED_PORES),
            Map.entry("mất nước", SkinConcern.DEHYDRATION),
            Map.entry("thiếu nước", SkinConcern.DEHYDRATION),
            Map.entry("xỉn màu", SkinConcern.DULLNESS),
            Map.entry("thiếu sức sống", SkinConcern.DULLNESS)
    );

    /**
     * @return a valid {@link SkinType} name; never null. Unknown/blank/"UNKNOWN" -> NORMAL.
     */
    public String normalizeSkinType(String raw) {
        if (raw == null) {
            return SkinType.NORMAL.name();
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            return SkinType.NORMAL.name();
        }
        for (SkinType type : SkinType.values()) {
            if (type.name().equalsIgnoreCase(trimmed)) {
                return type.name();
            }
        }
        SkinType vn = SKIN_TYPE_VN.get(trimmed.toLowerCase(Locale.ROOT));
        if (vn != null) {
            return vn.name();
        }
        return SkinType.NORMAL.name();
    }

    /**
     * @return distinct valid {@link SkinConcern} names; unmappable entries are dropped.
     */
    public List<String> normalizeConcerns(List<String> raw) {
        if (raw == null || raw.isEmpty()) {
            return List.of();
        }
        Set<String> result = new LinkedHashSet<>();
        for (String c : raw) {
            String mapped = normalizeConcern(c);
            if (mapped != null) {
                result.add(mapped);
            }
        }
        return new ArrayList<>(result);
    }

    private String normalizeConcern(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        for (SkinConcern concern : SkinConcern.values()) {
            if (concern.name().equalsIgnoreCase(trimmed)) {
                return concern.name();
            }
        }
        SkinConcern vn = CONCERN_VN.get(trimmed.toLowerCase(Locale.ROOT));
        return vn != null ? vn.name() : null;
    }
}
