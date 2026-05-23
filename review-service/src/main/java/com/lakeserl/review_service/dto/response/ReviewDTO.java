package com.lakeserl.review_service.dto.response;

import com.lakeserl.review_service.enums.ReviewStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class ReviewDTO {
    private Long id;
    private Long productId;
    private UUID userId;
    private Long orderId;
    private Long orderItemId;
    private Short rating;
    private String title;
    private String body;
    private String skinType;
    private String ageRange;
    private Short skinEffectRating;
    private Short textureRating;
    private Short scentRating;
    private Short packagingRating;
    private Short valueRating;
    private boolean verified;
    private ReviewStatus status;
    private int helpfulCount;
    private int notHelpfulCount;
    private int reportCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<ReviewMediaDTO> media;
    private List<ReviewReplyDTO> replies;
    /** null if current user has not voted */
    private Boolean currentUserVote;
}

