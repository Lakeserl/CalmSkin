package com.lakeserl.ai_skin_analysis_service.ai;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * KI-1: the normalizer is the defensive layer that guarantees only canonical enum values
 * reach the DB / product-service / Kafka, even when Gemini returns Vietnamese, odd casing,
 * or "UNKNOWN". These tests encode WHY: a non-enum skin type must never leak downstream.
 */
class SkinAnalysisNormalizerTest {

    private final SkinAnalysisNormalizer normalizer = new SkinAnalysisNormalizer();

    @Test
    void mapsCanonicalEnumNamesRegardlessOfCaseOrWhitespace() {
        assertThat(normalizer.normalizeSkinType("OILY")).isEqualTo("OILY");
        assertThat(normalizer.normalizeSkinType("oily")).isEqualTo("OILY");
        assertThat(normalizer.normalizeSkinType("  Combination ")).isEqualTo("COMBINATION");
    }

    @Test
    void mapsVietnameseSkinTypeLabels() {
        assertThat(normalizer.normalizeSkinType("da dầu")).isEqualTo("OILY");
        assertThat(normalizer.normalizeSkinType("Da Dầu")).isEqualTo("OILY");
        assertThat(normalizer.normalizeSkinType("da nhờn")).isEqualTo("OILY");
        assertThat(normalizer.normalizeSkinType("da khô")).isEqualTo("DRY");
        assertThat(normalizer.normalizeSkinType("da hỗn hợp")).isEqualTo("COMBINATION");
        assertThat(normalizer.normalizeSkinType("da nhạy cảm")).isEqualTo("SENSITIVE");
        assertThat(normalizer.normalizeSkinType("da thường")).isEqualTo("NORMAL");
    }

    @Test
    void unknownBlankOrNullSkinTypeFallsBackToNormal() {
        // "UNKNOWN" is exactly what the Gemini error path emits — must not leak downstream.
        assertThat(normalizer.normalizeSkinType("UNKNOWN")).isEqualTo("NORMAL");
        assertThat(normalizer.normalizeSkinType("da gì đó")).isEqualTo("NORMAL");
        assertThat(normalizer.normalizeSkinType("")).isEqualTo("NORMAL");
        assertThat(normalizer.normalizeSkinType("   ")).isEqualTo("NORMAL");
        assertThat(normalizer.normalizeSkinType(null)).isEqualTo("NORMAL");
    }

    @Test
    void mapsConcernsEnglishAndVietnameseDedupedAndDropsUnmappable() {
        List<String> in = Arrays.asList("ACNE", "mụn", "nám", "không rõ", null, "  ", "wrinkles");
        // ACNE; "mụn"->ACNE (deduped); "nám"->DARK_SPOTS; junk/null/blank dropped; "wrinkles"->WRINKLES
        assertThat(normalizer.normalizeConcerns(in))
                .containsExactly("ACNE", "DARK_SPOTS", "WRINKLES");
    }

    @Test
    void nullOrEmptyConcernsYieldEmptyList() {
        assertThat(normalizer.normalizeConcerns(null)).isEmpty();
        assertThat(normalizer.normalizeConcerns(List.of())).isEmpty();
    }
}
