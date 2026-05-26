package com.lakeserl.subscription_service.service;

import com.lakeserl.subscription_service.dto.request.CreateSubscriptionRequest;
import com.lakeserl.subscription_service.dto.request.UpdateSubscriptionRequest;
import com.lakeserl.subscription_service.dto.response.SubscriptionDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface SubscriptionService {

    SubscriptionDTO create(UUID userId, CreateSubscriptionRequest request);

    SubscriptionDTO getById(UUID id, UUID requestingUserId);

    Page<SubscriptionDTO> listByUser(UUID userId, Pageable pageable);

    SubscriptionDTO update(UUID id, UUID requestingUserId, UpdateSubscriptionRequest request);

    SubscriptionDTO pause(UUID id, UUID requestingUserId);

    SubscriptionDTO resume(UUID id, UUID requestingUserId);

    SubscriptionDTO cancel(UUID id, UUID requestingUserId);

    // Admin-only: get any subscription without ownership check
    SubscriptionDTO adminGetById(UUID id);

    // Admin-only: list all subscriptions, optionally filtered
    Page<SubscriptionDTO> adminListAll(Pageable pageable);
}
