package com.lakeserl.ai_recommendation_service.service;

import com.lakeserl.ai_recommendation_service.client.ProductServiceClient;
import com.lakeserl.ai_recommendation_service.client.UserServiceClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.Mockito.mock;

/**
 * Content-based "similar" ranking relies on Jaccard similarity over product attribute sets.
 * The ranking is only meaningful if identical sets score 1.0, disjoint sets 0.0, and partial
 * overlap exactly intersection/union — otherwise "similar" products are mis-ordered.
 */
class RecommendationServiceImplTest {

    private RecommendationServiceImpl service;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        service = new RecommendationServiceImpl(
                mock(ProductServiceClient.class), mock(UserServiceClient.class), mock(RedisTemplate.class));
    }

    private double jaccard(Set<String> a, Set<String> b) {
        Double v = ReflectionTestUtils.invokeMethod(service, "jaccardSimilarity", a, b);
        return v == null ? Double.NaN : v;
    }

    @Test
    void identicalAttributeSetsScoreOne() {
        assertThat(jaccard(Set.of("OILY", "ACNE"), Set.of("OILY", "ACNE"))).isEqualTo(1.0);
    }

    @Test
    void disjointAttributeSetsScoreZero() {
        assertThat(jaccard(Set.of("OILY"), Set.of("DRY"))).isEqualTo(0.0);
    }

    @Test
    void partialOverlapScoresIntersectionOverUnion() {
        // {A,B,C} vs {B,C,D}: intersection {B,C}=2, union {A,B,C,D}=4 -> 0.5
        assertThat(jaccard(Set.of("A", "B", "C"), Set.of("B", "C", "D"))).isCloseTo(0.5, within(1e-9));
    }

    @Test
    void twoEmptySetsScoreOne() {
        assertThat(jaccard(Set.of(), Set.of())).isEqualTo(1.0);
    }
}
