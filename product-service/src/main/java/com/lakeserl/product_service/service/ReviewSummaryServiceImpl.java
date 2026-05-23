package com.lakeserl.product_service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lakeserl.product_service.dto.request.UpdateReviewSummaryRequest;
import com.lakeserl.product_service.entity.ProcessedKafkaEvent;
import com.lakeserl.product_service.entity.Product;
import com.lakeserl.product_service.entity.ReviewSummary;
import com.lakeserl.product_service.repository.ProcessedKafkaEventRepository;
import com.lakeserl.product_service.repository.ProductRepository;
import com.lakeserl.product_service.repository.ReviewSummaryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
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
    private final ProcessedKafkaEventRepository processedKafkaEventRepository;
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

    @KafkaListener(topics = "review.summary.updated", groupId = "product-service")
    @Transactional
    public void consumeReviewSummaryUpdate(ConsumerRecord<String, Map<String, Object>> record) {
        String eventId = record.topic() + ":" + (record.key() != null ? record.key() : record.offset());
        if (processedKafkaEventRepository.existsById(eventId)) {
            log.debug("Skipping duplicate review.summary.updated event: {}", eventId);
            return;
        }
        try {
            UpdateReviewSummaryRequest request = objectMapper.convertValue(record.value(), UpdateReviewSummaryRequest.class);
            handleReviewSummaryUpdate(request);
            processedKafkaEventRepository.save(new ProcessedKafkaEvent(eventId, "review.summary.updated", null));
        } catch (Exception e) {
            log.error("Error processing review summary update from Kafka", e);
        }
    }
}
