package com.lakeserl.product_service.service;

import com.lakeserl.product_service.client.OrderServiceClient;
import com.lakeserl.product_service.client.UserServiceClient;
import com.lakeserl.product_service.dto.response.ProductSummaryDTO;
import com.lakeserl.product_service.entity.Product;
import com.lakeserl.product_service.mapper.ProductMapper;
import com.lakeserl.product_service.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Rule-based recommendation engine (v1, no ML).
 *
 * Algorithm:
 * 1. Fetch user's skin profile (skinType, skinConcerns) via user-service internal API.
 * 2. Fetch purchased productIds (last 365 days) via order-service internal API.
 * 3. Fetch wishlist productIds via user-service internal API.
 * 4. Determine brand affinity: find the most-purchased brand from purchased products.
 * 5. Query products matching skinType OR any skinConcern, excluding already-purchased ones,
 *    boosting by brand affinity, sorted by rating DESC then soldCount DESC.
 * 6. Map to ProductSummaryDTO using existing ProductMapper.toSummaryDto.
 *
 * Graceful degradation: if any upstream call fails, the service still returns results using
 * whatever data it has (empty exclusion list, no brand boost, no skin filter).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationService {

    private static final int DEFAULT_LIMIT = 10;
    private static final int MAX_LIMIT = 20;

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;
    private final UserServiceClient userServiceClient;
    private final OrderServiceClient orderServiceClient;

    @Transactional(readOnly = true)
    public List<ProductSummaryDTO> getRecommendations(UUID userId, int limit) {
        int safeLimit = Math.max(1, Math.min(limit, MAX_LIMIT));

        // 1. Skin profile (may be null if user hasn't filled it out)
        UserServiceClient.SkinProfileData skinProfile = userServiceClient.getSkinProfile(userId);

        // 2. Purchased productIds — used to exclude from results
        List<Long> purchasedIds = orderServiceClient.getPurchasedProductIds(userId);

        // 3. Wishlist productIds — also excluded from recommendations to present new products
        List<Long> wishlistIds = userServiceClient.getWishlistProductIds(userId);

        // 4. Extract skin signals
        String skinType = null;
        String concern1 = null;
        String concern2 = null;
        String concern3 = null;

        if (skinProfile != null) {
            skinType = skinProfile.getSkinType();
            List<String> concerns = skinProfile.getSkinConcerns();
            if (concerns != null) {
                if (concerns.size() > 0) concern1 = concerns.get(0);
                if (concerns.size() > 1) concern2 = concerns.get(1);
                if (concerns.size() > 2) concern3 = concerns.get(2);
            }
        }

        // 5. Brand affinity: find the brand ID appearing most in purchased products
        Long boostBrandId = determineBrandAffinity(purchasedIds);

        // 6. Exclusion list (purchased + wishlisted products): must not be empty for IN clause — pass null if empty
        java.util.Set<Long> excludeSet = new java.util.HashSet<>(purchasedIds);
        excludeSet.addAll(wishlistIds);
        List<Long> excludeIds = excludeSet.isEmpty() ? null : new java.util.ArrayList<>(excludeSet);

        // 7. Query
        log.debug("Recommendations for userId={}: skinType={}, concern1={}, excludeCount={}, boostBrand={}",
                userId, skinType, concern1, excludeIds == null ? 0 : excludeIds.size(), boostBrandId);

        List<Product> products = productRepository.findRecommendations(
                skinType, concern1, concern2, concern3,
                excludeIds, boostBrandId, safeLimit);

        // 8. Fallback: if no results (user has no skin profile or all products purchased),
        //    return best sellers instead so the endpoint never returns an empty list.
        if (products.isEmpty()) {
            log.debug("No personalized recommendations for userId={}; falling back to best sellers", userId);
            products = productRepository.findBestSellers(
                    org.springframework.data.domain.PageRequest.of(0, safeLimit)).getContent();
        }

        return products.stream()
                .map(productMapper::toSummaryDto)
                .collect(Collectors.toList());
    }

    /**
     * Determines brand affinity by finding the brand whose products appear most in the purchased list.
     * Fetches the products and counts brand occurrences. Returns null if no purchased products.
     */
    private Long determineBrandAffinity(List<Long> purchasedIds) {
        if (purchasedIds == null || purchasedIds.isEmpty()) {
            return null;
        }
        try {
            List<Product> purchasedProducts = productRepository.findAllById(purchasedIds);
            return purchasedProducts.stream()
                    .filter(p -> p.getBrand() != null)
                    .collect(Collectors.groupingBy(
                            p -> p.getBrand().getId(),
                            Collectors.counting()))
                    .entrySet().stream()
                    .max(java.util.Map.Entry.comparingByValue())
                    .map(java.util.Map.Entry::getKey)
                    .orElse(null);
        } catch (Exception e) {
            log.warn("Failed to determine brand affinity: {}", e.getMessage());
            return null;
        }
    }
}
