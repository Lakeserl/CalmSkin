package com.lakeserl.product_service.service;

import com.lakeserl.product_service.client.UserServiceClient;
import com.lakeserl.product_service.dto.request.GenerateRoutineRequest;
import com.lakeserl.product_service.dto.response.ProductSummaryDTO;
import com.lakeserl.product_service.dto.response.RoutineResponse;
import com.lakeserl.product_service.entity.Product;
import com.lakeserl.product_service.enums.ProductUsageStep;
import com.lakeserl.product_service.mapper.ProductMapper;
import com.lakeserl.product_service.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoutineService {

    private final ProductRepository productRepository;
    private final UserServiceClient userServiceClient;
    private final ProductMapper productMapper;

    public RoutineResponse generateRoutine(GenerateRoutineRequest request, UUID userId) {
        String skinType = request != null ? request.getSkinType() : null;
        List<String> concerns = request != null ? request.getSkinConcerns() : null;

        // Fallback to fetch skin profile from user-service if not supplied in request and userId is available
        if ((skinType == null || skinType.isBlank()) && userId != null) {
            try {
                UserServiceClient.SkinProfileData profile = userServiceClient.getSkinProfile(userId);
                if (profile != null) {
                    skinType = profile.getSkinType();
                    concerns = profile.getSkinConcerns();
                    log.info("Fetched skin profile from user-service for userId={}: skinType={}, concerns={}", 
                            userId, skinType, concerns);
                }
            } catch (Exception e) {
                log.warn("Failed to fetch skin profile for user {}: {}", userId, e.getMessage());
            }
        }

        // Extract concerns to individual variables for native query
        String concern1 = null;
        String concern2 = null;
        String concern3 = null;
        if (concerns != null) {
            if (concerns.size() > 0) concern1 = concerns.get(0);
            if (concerns.size() > 1) concern2 = concerns.get(1);
            if (concerns.size() > 2) concern3 = concerns.get(2);
        }

        // Fetch products matching each usage step
        List<ProductSummaryDTO> cleanseProducts = fetchAndMapProducts(ProductUsageStep.CLEANSE.name(), skinType, concern1, concern2, concern3, 2);
        List<ProductSummaryDTO> toneProducts = fetchAndMapProducts(ProductUsageStep.TONE.name(), skinType, concern1, concern2, concern3, 1);
        List<ProductSummaryDTO> serumProducts = fetchAndMapProducts(ProductUsageStep.SERUM.name(), skinType, concern1, concern2, concern3, 2);
        List<ProductSummaryDTO> moisturizeProducts = fetchAndMapProducts(ProductUsageStep.MOISTURIZE.name(), skinType, concern1, concern2, concern3, 2);
        List<ProductSummaryDTO> spfProducts = fetchAndMapProducts(ProductUsageStep.SPF.name(), skinType, concern1, concern2, concern3, 2);

        // Build Morning Routine
        // Morning Treat: Tone first, then Serum (or combine them)
        List<ProductSummaryDTO> morningTreat = new ArrayList<>();
        if (!toneProducts.isEmpty()) morningTreat.add(toneProducts.get(0));
        if (!serumProducts.isEmpty()) morningTreat.add(serumProducts.get(0));

        RoutineResponse.RoutineSteps morning = new RoutineResponse.RoutineSteps(
                cleanseProducts.isEmpty() ? Collections.emptyList() : Collections.singletonList(cleanseProducts.get(0)),
                morningTreat,
                moisturizeProducts.isEmpty() ? Collections.emptyList() : Collections.singletonList(moisturizeProducts.get(0)),
                spfProducts.isEmpty() ? Collections.emptyList() : Collections.singletonList(spfProducts.get(0))
        );

        // Build Evening Routine
        // Evening Treat: Toner + Serum (can include multiple serums)
        List<ProductSummaryDTO> eveningTreat = new ArrayList<>();
        if (!toneProducts.isEmpty()) eveningTreat.add(toneProducts.get(0));
        if (serumProducts.size() > 0) eveningTreat.add(serumProducts.get(0));
        if (serumProducts.size() > 1) eveningTreat.add(serumProducts.get(1));

        // Evening moisturize: we can pick the second moisturizing option if available (like night cream), or fallback to first
        List<ProductSummaryDTO> eveningMoisturize = new ArrayList<>();
        if (moisturizeProducts.size() > 1) {
            eveningMoisturize.add(moisturizeProducts.get(1));
        } else if (moisturizeProducts.size() > 0) {
            eveningMoisturize.add(moisturizeProducts.get(0));
        }

        RoutineResponse.RoutineSteps evening = new RoutineResponse.RoutineSteps(
                cleanseProducts.isEmpty() ? Collections.emptyList() : Collections.singletonList(cleanseProducts.get(0)),
                eveningTreat,
                eveningMoisturize,
                Collections.emptyList() // No SPF at night
        );

        return new RoutineResponse(morning, evening);
    }

    private List<ProductSummaryDTO> fetchAndMapProducts(String usageStep, String skinType, String concern1, String concern2, String concern3, int limit) {
        List<Product> products = productRepository.findRoutineProducts(usageStep, skinType, concern1, concern2, concern3, limit);
        return products.stream()
                .map(productMapper::toSummaryDto)
                .collect(Collectors.toList());
    }
}
