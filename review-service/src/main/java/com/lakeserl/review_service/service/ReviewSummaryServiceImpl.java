package com.lakeserl.review_service.service;

import com.lakeserl.review_service.dto.response.ReviewSummaryDTO;
import com.lakeserl.review_service.entity.ProductReviewSummary;
import com.lakeserl.review_service.entity.Review;
import com.lakeserl.review_service.enums.ReviewStatus;
import com.lakeserl.review_service.repository.ProductReviewSummaryRepository;
import com.lakeserl.review_service.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewSummaryServiceImpl implements ReviewSummaryService {

    private final ReviewRepository reviewRepository;
    private final ProductReviewSummaryRepository summaryRepository;

    @Override
    @Transactional(readOnly = true)
    public ReviewSummaryDTO getSummary(Long productId) {
        return summaryRepository.findById(productId)
                .map(this::toDTO)
                .orElseGet(() -> ReviewSummaryDTO.builder()
                        .productId(productId)
                        .totalCount(0)
                        .averageRating(BigDecimal.ZERO)
                        .build());
    }

    @Async
    @Override
    @Transactional
    public void rebuildSummary(Long productId) {
        List<Review> reviews = reviewRepository
                .findByFilters(productId, ReviewStatus.PUBLISHED, null, null, Pageable.unpaged())
                .getContent();

        if (reviews.isEmpty()) {
            summaryRepository.deleteById(productId);
            return;
        }

        int total = reviews.size();
        long sum = reviews.stream().mapToLong(r -> r.getRating()).sum();
        BigDecimal avg = BigDecimal.valueOf(sum).divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);

        ProductReviewSummary summary = summaryRepository.findById(productId)
                .orElseGet(() -> ProductReviewSummary.builder().productId(productId).build());

        summary.setTotalCount(total);
        summary.setAverageRating(avg);
        summary.setCount1star((int) reviews.stream().filter(r -> r.getRating() == 1).count());
        summary.setCount2star((int) reviews.stream().filter(r -> r.getRating() == 2).count());
        summary.setCount3star((int) reviews.stream().filter(r -> r.getRating() == 3).count());
        summary.setCount4star((int) reviews.stream().filter(r -> r.getRating() == 4).count());
        summary.setCount5star((int) reviews.stream().filter(r -> r.getRating() == 5).count());
        summary.setCountOily((int) reviews.stream().filter(r -> "OILY".equals(r.getSkinType())).count());
        summary.setCountDry((int) reviews.stream().filter(r -> "DRY".equals(r.getSkinType())).count());
        summary.setCountCombination((int) reviews.stream().filter(r -> "COMBINATION".equals(r.getSkinType())).count());
        summary.setCountSensitive((int) reviews.stream().filter(r -> "SENSITIVE".equals(r.getSkinType())).count());
        summary.setCountNormal((int) reviews.stream().filter(r -> "NORMAL".equals(r.getSkinType())).count());
        summary.setAvgSkinEffect(averageRating(reviews, "SKIN_EFFECT"));
        summary.setAvgTexture(averageRating(reviews, "TEXTURE"));
        summary.setAvgScent(averageRating(reviews, "SCENT"));
        summary.setAvgPackaging(averageRating(reviews, "PACKAGING"));
        summary.setAvgValue(averageRating(reviews, "VALUE"));

        summaryRepository.save(summary);
        log.info("Rebuilt review summary for productId={} total={} avg={}", productId, total, avg);
    }

    private BigDecimal averageRating(List<Review> reviews, String field) {
        double avg = reviews.stream()
                .mapToInt(r -> switch (field) {
                    case "SKIN_EFFECT" -> r.getSkinEffectRating() != null ? r.getSkinEffectRating() : 0;
                    case "TEXTURE" -> r.getTextureRating() != null ? r.getTextureRating() : 0;
                    case "SCENT" -> r.getScentRating() != null ? r.getScentRating() : 0;
                    case "PACKAGING" -> r.getPackagingRating() != null ? r.getPackagingRating() : 0;
                    case "VALUE" -> r.getValueRating() != null ? r.getValueRating() : 0;
                    default -> 0;
                })
                .filter(v -> v > 0)
                .average()
                .orElse(0.0);
        return avg == 0.0 ? null : BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP);
    }

    private ReviewSummaryDTO toDTO(ProductReviewSummary s) {
        return ReviewSummaryDTO.builder()
                .productId(s.getProductId())
                .totalCount(s.getTotalCount())
                .averageRating(s.getAverageRating())
                .count1star(s.getCount1star())
                .count2star(s.getCount2star())
                .count3star(s.getCount3star())
                .count4star(s.getCount4star())
                .count5star(s.getCount5star())
                .countOily(s.getCountOily())
                .countDry(s.getCountDry())
                .countCombination(s.getCountCombination())
                .countSensitive(s.getCountSensitive())
                .countNormal(s.getCountNormal())
                .avgSkinEffect(s.getAvgSkinEffect())
                .avgTexture(s.getAvgTexture())
                .avgScent(s.getAvgScent())
                .avgPackaging(s.getAvgPackaging())
                .avgValue(s.getAvgValue())
                .build();
    }
}
