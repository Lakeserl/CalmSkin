package com.lakeserl.subscription_service.service;

import com.lakeserl.subscription_service.dto.request.CreateSubscriptionRequest;
import com.lakeserl.subscription_service.dto.request.UpdateSubscriptionRequest;
import com.lakeserl.subscription_service.dto.response.SubscriptionDTO;
import com.lakeserl.subscription_service.entity.Subscription;
import com.lakeserl.subscription_service.enums.SubscriptionStatus;
import com.lakeserl.subscription_service.exception.InvalidSubscriptionStateException;
import com.lakeserl.subscription_service.exception.SubscriptionNotFoundException;
import com.lakeserl.subscription_service.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionServiceImpl implements SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionMapper mapper;

    @Override
    @Transactional
    public SubscriptionDTO create(UUID userId, CreateSubscriptionRequest request) {
        LocalDateTime now = LocalDateTime.now();
        Subscription subscription = Subscription.builder()
                .userId(userId)
                .productId(request.productId())
                .frequencyDays(request.frequencyDays())
                .addressId(request.addressId())
                .status(SubscriptionStatus.ACTIVE)
                // First order is due immediately (today) so the scheduler picks it up tonight/tomorrow
                .nextOrderDueAt(now)
                .build();

        subscription = subscriptionRepository.save(subscription);
        log.info("Created subscription id={} for userId={} productId={} freq={}d",
                subscription.getId(), userId, request.productId(), request.frequencyDays());
        return mapper.toDto(subscription);
    }

    @Override
    @Transactional(readOnly = true)
    public SubscriptionDTO getById(UUID id, UUID requestingUserId) {
        Subscription subscription = findOwnedSubscription(id, requestingUserId);
        return mapper.toDto(subscription);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SubscriptionDTO> listByUser(UUID userId, Pageable pageable) {
        return subscriptionRepository.findByUserId(userId, pageable)
                .map(mapper::toDto);
    }

    @Override
    @Transactional
    public SubscriptionDTO update(UUID id, UUID requestingUserId, UpdateSubscriptionRequest request) {
        Subscription subscription = findOwnedSubscription(id, requestingUserId);

        if (!subscription.getStatus().isModifiable()) {
            throw new InvalidSubscriptionStateException(
                    "Cannot update subscription in status " + subscription.getStatus());
        }

        boolean changed = false;
        if (request.frequencyDays() != null) {
            subscription.setFrequencyDays(request.frequencyDays());
            changed = true;
        }
        if (request.addressId() != null) {
            subscription.setAddressId(request.addressId());
            changed = true;
        }

        if (changed) {
            subscription = subscriptionRepository.save(subscription);
            log.info("Updated subscription id={}", id);
        }
        return mapper.toDto(subscription);
    }

    @Override
    @Transactional
    public SubscriptionDTO pause(UUID id, UUID requestingUserId) {
        Subscription subscription = findOwnedSubscription(id, requestingUserId);

        if (!subscription.getStatus().isActive()) {
            throw new InvalidSubscriptionStateException(
                    "Cannot pause subscription in status " + subscription.getStatus());
        }
        subscription.setStatus(SubscriptionStatus.PAUSED);
        subscription = subscriptionRepository.save(subscription);
        log.info("Paused subscription id={}", id);
        return mapper.toDto(subscription);
    }

    @Override
    @Transactional
    public SubscriptionDTO resume(UUID id, UUID requestingUserId) {
        Subscription subscription = findOwnedSubscription(id, requestingUserId);

        if (!subscription.getStatus().isPaused()) {
            throw new InvalidSubscriptionStateException(
                    "Cannot resume subscription in status " + subscription.getStatus());
        }
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        // Recalculate next order due from now, not from original schedule,
        // to avoid a burst of "missed" orders after a long pause.
        subscription.setNextOrderDueAt(LocalDateTime.now().plusDays(subscription.getFrequencyDays()));
        subscription = subscriptionRepository.save(subscription);
        log.info("Resumed subscription id={} nextDue={}", id, subscription.getNextOrderDueAt());
        return mapper.toDto(subscription);
    }

    @Override
    @Transactional
    public SubscriptionDTO cancel(UUID id, UUID requestingUserId) {
        Subscription subscription = findOwnedSubscription(id, requestingUserId);

        if (subscription.getStatus().isCancelled()) {
            throw new InvalidSubscriptionStateException("Subscription is already cancelled");
        }
        subscription.setStatus(SubscriptionStatus.CANCELLED);
        subscription = subscriptionRepository.save(subscription);
        log.info("Cancelled subscription id={}", id);
        return mapper.toDto(subscription);
    }

    @Override
    @Transactional(readOnly = true)
    public SubscriptionDTO adminGetById(UUID id) {
        return mapper.toDto(findById(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SubscriptionDTO> adminListAll(Pageable pageable) {
        return subscriptionRepository.findAll(pageable).map(mapper::toDto);
    }

    // ─── helpers ──────────────────────────────────────────────────────────────

    private Subscription findById(UUID id) {
        return subscriptionRepository.findById(id)
                .orElseThrow(() -> new SubscriptionNotFoundException("Subscription not found: " + id));
    }

    /**
     * Returns the subscription only if it belongs to {@code requestingUserId}.
     * Returns 404 (via {@link SubscriptionNotFoundException}) rather than 403
     * to avoid leaking resource existence (IDOR mitigation).
     */
    private Subscription findOwnedSubscription(UUID id, UUID requestingUserId) {
        return subscriptionRepository.findByIdAndUserId(id, requestingUserId)
                .orElseThrow(() -> new SubscriptionNotFoundException("Subscription not found: " + id));
    }
}
