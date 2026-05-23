package com.lakeserl.user_service.service;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lakeserl.user_service.exception.UserNotFoundException;
import com.lakeserl.user_service.model.entity.PointTransaction;
import com.lakeserl.user_service.model.entity.User;
import com.lakeserl.user_service.model.entity.UserPoint;
import com.lakeserl.user_service.model.enums.LoyaltyTier;
import com.lakeserl.user_service.model.enums.PointTransactionType;
import com.lakeserl.user_service.repository.PointTransactionRepository;
import com.lakeserl.user_service.repository.UserPointRepository;
import com.lakeserl.user_service.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoyaltyService {

    private final UserPointRepository userPointRepository;
    private final PointTransactionRepository pointTransactionRepository;
    private final UserRepository userRepository;

    public UserPoint getPoints(UUID userId) {
        return userPointRepository.findByUserId(userId)
                .orElseGet(() -> initPoints(userId));
    }

    public Page<PointTransaction> getTransactions(UUID userId, Pageable pageable) {
        return pointTransactionRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    @Transactional
    public void earnPoints(UUID userId, int points, String referenceId, String referenceType, String description) {
        if (pointTransactionRepository.existsByUserIdAndReferenceIdAndReferenceTypeAndType(
                userId, referenceId, referenceType, PointTransactionType.EARN)) {
            log.warn("Duplicate earnPoints skipped: userId={} referenceId={}", userId, referenceId);
            return;
        }

        UserPoint userPoint = getPoints(userId);
        userPoint.setTotalPoints(userPoint.getTotalPoints() + points);
        userPoint.setTier(calculateTier(userPoint.getTotalPoints()));
        userPointRepository.save(userPoint);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        pointTransactionRepository.save(PointTransaction.builder()
                .user(user)
                .points(points)
                .type(PointTransactionType.EARN)
                .referenceId(referenceId)
                .referenceType(referenceType)
                .description(description)
                .build());
    }

    @Transactional
    public void redeemPoints(UUID userId, int points, String referenceId, String referenceType, String description) {
        if (pointTransactionRepository.existsByUserIdAndReferenceIdAndReferenceTypeAndType(
                userId, referenceId, referenceType, PointTransactionType.REDEEM)) {
            log.warn("Duplicate redeemPoints skipped: userId={} referenceId={}", userId, referenceId);
            return;
        }

        UserPoint userPoint = getPoints(userId);
        userPoint.setTotalPoints(Math.max(0, userPoint.getTotalPoints() - points));
        userPoint.setTier(calculateTier(userPoint.getTotalPoints()));
        userPointRepository.save(userPoint);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        pointTransactionRepository.save(PointTransaction.builder()
                .user(user)
                .points(points)
                .type(PointTransactionType.REDEEM)
                .referenceId(referenceId)
                .referenceType(referenceType)
                .description(description)
                .build());
    }

    private UserPoint initPoints(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
        return userPointRepository.save(UserPoint.builder()
                .user(user).totalPoints(0).tier(LoyaltyTier.BRONZE).build());
    }

    private LoyaltyTier calculateTier(int totalPoints) {
        if (totalPoints >= 10000) return LoyaltyTier.PLATINUM;
        if (totalPoints >= 5000) return LoyaltyTier.GOLD;
        if (totalPoints >= 1000) return LoyaltyTier.SILVER;
        return LoyaltyTier.BRONZE;
    }
}
