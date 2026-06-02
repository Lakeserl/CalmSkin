package com.lakeserl.ai_recommendation_service.service;

import com.lakeserl.ai_recommendation_service.client.ProductServiceClient;
import com.lakeserl.ai_recommendation_service.client.UserServiceClient;
import com.lakeserl.ai_recommendation_service.dto.ApiResponse;
import com.lakeserl.ai_recommendation_service.dto.ProductDTO;
import com.lakeserl.ai_recommendation_service.dto.RecommendationResponseDTO;
import com.lakeserl.ai_recommendation_service.dto.UserSkinProfileDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationServiceImpl implements RecommendationService {

    private static final String TRENDING_KEY   = "rec:trending";
    private static final String SIMILAR_PREFIX = "rec:similar:";

    private final ProductServiceClient productServiceClient;
    private final UserServiceClient userServiceClient;
    private final RedisTemplate<String, String> redisTemplate;

    @Value("${app.internal.secret}")
    private String internalSecret;

    @Override
    public RecommendationResponseDTO forMe(UUID userId) {
        try {
            ApiResponse<UserSkinProfileDTO> profileResponse =
                    userServiceClient.getSkinProfile(userId, internalSecret);
            if (profileResponse == null || profileResponse.getData() == null) {
                return trending(10);
            }
            UserSkinProfileDTO profile = profileResponse.getData();
            ApiResponse<Map<String, List<ProductDTO>>> result =
                    productServiceClient.getBySkinProfile(profile, internalSecret);

            List<ProductDTO> products = result != null && result.getData() != null
                    ? result.getData().values().stream().flatMap(List::stream)
                              .limit(10).collect(Collectors.toList())
                    : List.of();

            return RecommendationResponseDTO.builder()
                    .strategy("SKIN_PROFILE")
                    .products(products)
                    .build();
        } catch (Exception e) {
            log.warn("forMe fallback for userId={}: {}", userId, e.getMessage());
            return trending(10);
        }
    }

    @Override
    public RecommendationResponseDTO similar(Long productId) {
        String cacheKey = SIMILAR_PREFIX + productId;
        // Return cached similar product IDs from Redis if available
        List<String> cachedIds = redisTemplate.opsForList().range(cacheKey, 0, 9);
        if (cachedIds != null && !cachedIds.isEmpty()) {
            List<Long> ids = cachedIds.stream().map(Long::valueOf).collect(Collectors.toList());
            return fetchAndBuildResponse("SIMILAR", ids);
        }

        try {
            ApiResponse<ProductDTO> resp = productServiceClient.getById(productId, internalSecret);
            if (resp == null || resp.getData() == null) {
                return RecommendationResponseDTO.builder().strategy("SIMILAR").products(List.of()).build();
            }
            ProductDTO target = resp.getData();
            Set<String> targetAttributes = buildAttributeSet(target);

            // Compute Jaccard similarity against a broader batch — fetch top products for target skinTypes
            // For MVP: reuse by-skin-profile endpoint with target product's skin attributes
            UserSkinProfileDTO syntheticProfile = new UserSkinProfileDTO();
            syntheticProfile.setSkinType(target.getSkinTypes() != null && !target.getSkinTypes().isEmpty()
                    ? target.getSkinTypes().get(0) : null);
            syntheticProfile.setSkinConcerns(target.getSkinConcerns());
            syntheticProfile.setAllergies(List.of());

            ApiResponse<Map<String, List<ProductDTO>>> poolResponse =
                    productServiceClient.getBySkinProfile(syntheticProfile, internalSecret);
            List<ProductDTO> pool = poolResponse != null && poolResponse.getData() != null
                    ? poolResponse.getData().values().stream().flatMap(List::stream).collect(Collectors.toList())
                    : List.of();

            List<Long> similarIds = pool.stream()
                    .filter(p -> !p.getId().equals(productId))
                    .sorted((a, b) -> Double.compare(
                            jaccardSimilarity(buildAttributeSet(b), targetAttributes),
                            jaccardSimilarity(buildAttributeSet(a), targetAttributes)))
                    .limit(10)
                    .map(ProductDTO::getId)
                    .collect(Collectors.toList());

            if (!similarIds.isEmpty()) {
                List<String> toCache = similarIds.stream().map(String::valueOf).collect(Collectors.toList());
                redisTemplate.opsForList().rightPushAll(cacheKey, toCache);
                redisTemplate.expire(cacheKey, Duration.ofHours(6));
            }

            return fetchAndBuildResponse("SIMILAR", similarIds);
        } catch (Exception e) {
            log.warn("similar fallback for productId={}: {}", productId, e.getMessage());
            return RecommendationResponseDTO.builder().strategy("SIMILAR").products(List.of()).build();
        }
    }

    @Override
    public RecommendationResponseDTO trending(int limit) {
        Set<String> topIds = redisTemplate.opsForZSet().reverseRange(TRENDING_KEY, 0, Math.min(limit, 20) - 1);
        if (topIds == null || topIds.isEmpty()) {
            return RecommendationResponseDTO.builder().strategy("TRENDING").products(List.of()).build();
        }
        List<Long> ids = topIds.stream().map(Long::valueOf).collect(Collectors.toList());
        return fetchAndBuildResponse("TRENDING", ids);
    }

    @Override
    public RecommendationResponseDTO frequentlyBoughtWith(Long productId) {
        // MVP: delegate to similar — replace with market-basket analysis when order data is sufficient
        return similar(productId);
    }

    private RecommendationResponseDTO fetchAndBuildResponse(String strategy, List<Long> ids) {
        if (ids.isEmpty()) {
            return RecommendationResponseDTO.builder().strategy(strategy).products(List.of()).build();
        }
        try {
            ApiResponse<List<ProductDTO>> resp = productServiceClient.getBatch(ids, internalSecret);
            List<ProductDTO> products = resp != null && resp.getData() != null ? resp.getData() : List.of();
            return RecommendationResponseDTO.builder().strategy(strategy).products(products).build();
        } catch (Exception e) {
            log.warn("fetchAndBuildResponse failed for strategy={}: {}", strategy, e.getMessage());
            return RecommendationResponseDTO.builder().strategy(strategy).products(List.of()).build();
        }
    }

    private Set<String> buildAttributeSet(ProductDTO p) {
        Set<String> attrs = new HashSet<>();
        if (p.getSkinTypes() != null) attrs.addAll(p.getSkinTypes());
        if (p.getSkinConcerns() != null) attrs.addAll(p.getSkinConcerns());
        if (p.getCategoryName() != null) attrs.add(p.getCategoryName());
        return attrs;
    }

    private double jaccardSimilarity(Set<String> a, Set<String> b) {
        if (a.isEmpty() && b.isEmpty()) return 1.0;
        Set<String> intersection = new HashSet<>(a);
        intersection.retainAll(b);
        Set<String> union = new HashSet<>(a);
        union.addAll(b);
        return union.isEmpty() ? 0.0 : (double) intersection.size() / union.size();
    }
}
