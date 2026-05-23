package com.lakeserl.review_service.service;

import com.lakeserl.review_service.dto.response.EligibilityDTO;
import com.lakeserl.review_service.repository.ReviewEligibilityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReviewEligibilityService {

    private final ReviewEligibilityRepository eligibilityRepository;

    @Transactional(readOnly = true)
    public List<EligibilityDTO> getEligibleItems(UUID userId) {
        return eligibilityRepository.findEligibleByUserId(userId).stream()
                .map(e -> EligibilityDTO.builder()
                        .orderItemId(e.getOrderItemId())
                        .productId(e.getProductId())
                        .orderCompletedAt(e.getOrderCompletedAt())
                        .reviewed(e.getReviewId() != null)
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<EligibilityDTO> getAllEligibility(UUID userId) {
        return eligibilityRepository.findByUserId(userId).stream()
                .map(e -> EligibilityDTO.builder()
                        .orderItemId(e.getOrderItemId())
                        .productId(e.getProductId())
                        .orderCompletedAt(e.getOrderCompletedAt())
                        .reviewed(e.getReviewId() != null)
                        .build())
                .collect(Collectors.toList());
    }
}

