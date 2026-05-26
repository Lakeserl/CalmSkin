package com.lakeserl.subscription_service.service;

import com.lakeserl.subscription_service.dto.response.SubscriptionDTO;
import com.lakeserl.subscription_service.entity.Subscription;
import org.springframework.stereotype.Component;

@Component
public class SubscriptionMapper {

    public SubscriptionDTO toDto(Subscription s) {
        return SubscriptionDTO.builder()
                .id(s.getId())
                .userId(s.getUserId())
                .productId(s.getProductId())
                .frequencyDays(s.getFrequencyDays())
                .addressId(s.getAddressId())
                .status(s.getStatus())
                .lastOrderedAt(s.getLastOrderedAt())
                .nextOrderDueAt(s.getNextOrderDueAt())
                .createdAt(s.getCreatedAt())
                .updatedAt(s.getUpdatedAt())
                .build();
    }
}
