package com.lakeserl.subscription_service.dto.response;

import com.lakeserl.subscription_service.enums.SubscriptionStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class SubscriptionDTO {
    private UUID id;
    private UUID userId;
    private Long productId;
    private Integer frequencyDays;
    private UUID addressId;
    private SubscriptionStatus status;
    private LocalDateTime lastOrderedAt;
    private LocalDateTime nextOrderDueAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
