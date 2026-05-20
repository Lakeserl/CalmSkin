package com.lakeserl.product_service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lakeserl.product_service.dto.request.UpdateReviewSummaryRequest;
import com.lakeserl.product_service.entity.Product;
import com.lakeserl.product_service.entity.ReviewSummary;
import com.lakeserl.product_service.repository.ProductRepository;
import com.lakeserl.product_service.repository.ReviewSummaryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewSummaryServiceImpl implements ReviewSummaryService {

    private final ReviewSummaryRepository reviewSummaryRepository;
    private final ProductRepository productRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void handleReviewSummaryUpdate(UpdateReviewSummaryRequest request) {
        log.info("Processing review summary update for product: {}", request.getProductId());
        
        Product product = productRepository.findById(request.getProductId())
                .orElse(null);
                
        if (product == null) {
            log.warn("Product not found for review summary update: {}", request.getProductId());
            return;
        }

        ReviewSummary summary = reviewSummaryRepository.findByProductId(request.getProductId())
                .orElse(new ReviewSummary());

        if (summary.getId() == null) {
            summary.setProduct(product);
        }

        summary.setAverageRating(request.getAverageRating());
        summary.setTotalReviews(request.getTotalReviews());
        summary.setFiveStarCount(request.getFiveStarCount());
        summary.setFourStarCount(request.getFourStarCount());
        summary.setThreeStarCount(request.getThreeStarCount());
        summary.setTwoStarCount(request.getTwoStarCount());
        summary.setOneStarCount(request.getOneStarCount());

        reviewSummaryRepository.save(summary);
    }

    /**
     * Master Topic List §8: review-service publishes review.created / review.deleted,
     * each carrying the recomputed review summary snapshot for the affected product.
     */
    @KafkaListener(topics = {"review.created", "review.deleted"}, groupId = "product-service")
    public void consumeReviewSummaryUpdate(Map<String, Object> event) {
        try {
            UpdateReviewSummaryRequest request = objectMapper.convertValue(event, UpdateReviewSummaryRequest.class);
            handleReviewSummaryUpdate(request);
        } catch (Exception e) {
            log.error("Error processing review summary update from Kafka", e);
        }
    }
}
