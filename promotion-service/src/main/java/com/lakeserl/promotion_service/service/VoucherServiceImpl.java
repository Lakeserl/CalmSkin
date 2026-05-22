package com.lakeserl.promotion_service.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lakeserl.promotion_service.config.properties.PromotionProperties;
import com.lakeserl.promotion_service.dto.request.AssignBulkRequest;
import com.lakeserl.promotion_service.dto.request.VoucherValidationRequest;
import com.lakeserl.promotion_service.dto.response.MyVoucherResponse;
import com.lakeserl.promotion_service.dto.response.VoucherInfoResponse;
import com.lakeserl.promotion_service.dto.response.VoucherValidationResponse;
import com.lakeserl.promotion_service.entity.Promotion;
import com.lakeserl.promotion_service.entity.VoucherAssignment;
import com.lakeserl.promotion_service.enums.PromotionStatus;
import com.lakeserl.promotion_service.enums.PromotionType;
import com.lakeserl.promotion_service.enums.UsageStatus;
import com.lakeserl.promotion_service.exception.BadRequestException;
import com.lakeserl.promotion_service.exception.ConflictException;
import com.lakeserl.promotion_service.exception.ResourceNotFoundException;
import com.lakeserl.promotion_service.repository.PromotionRepository;
import com.lakeserl.promotion_service.repository.PromotionUsageRepository;
import com.lakeserl.promotion_service.repository.VoucherAssignmentRepository;
import com.lakeserl.promotion_service.service.engine.CartContext;
import com.lakeserl.promotion_service.service.engine.EvaluatedPromotion;
import com.lakeserl.promotion_service.service.engine.PromotionEvaluator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VoucherServiceImpl implements VoucherService {

    private final PromotionRepository promotionRepository;
    private final VoucherAssignmentRepository assignmentRepository;
    private final PromotionUsageRepository usageRepository;
    private final PromotionEvaluator evaluator;
    private final PromotionProperties properties;

    @Override
    public VoucherValidationResponse validate(VoucherValidationRequest request) {
        if (request.voucherCode() == null || request.voucherCode().isBlank()) {
            return VoucherValidationResponse.invalid("Voucher code is required");
        }
        Promotion promotion = promotionRepository
                .findByCode(request.voucherCode().trim().toUpperCase())
                .orElse(null);
        if (promotion == null) {
            return VoucherValidationResponse.invalid("Voucher code not found");
        }
        if (promotion.getType() != PromotionType.VOUCHER) {
            return VoucherValidationResponse.invalid("Code is not a voucher");
        }
        if (promotion.getStatus() != PromotionStatus.ACTIVE) {
            return VoucherValidationResponse.invalid("Voucher is not active");
        }
        CartContext context = new CartContext(
                request.userId(), null, null,
                request.subtotal() == null ? BigDecimal.ZERO : request.subtotal(),
                BigDecimal.ZERO);
        EvaluatedPromotion evaluated = evaluator.evaluate(promotion, context);
        if (!evaluated.eligible()) {
            return VoucherValidationResponse.invalid(evaluated.reason());
        }
        return VoucherValidationResponse.valid(evaluated.discount());
    }

    @Override
    public VoucherInfoResponse codeInfo(String code) {
        String normalized = code == null ? "" : code.trim().toUpperCase();
        Promotion promotion = promotionRepository.findByCode(normalized).orElse(null);
        if (promotion == null) {
            return new VoucherInfoResponse(normalized, null, null, null, null, null,
                    false, "Voucher code not found");
        }
        LocalDateTime now = LocalDateTime.now();
        boolean valid = promotion.getStatus() == PromotionStatus.ACTIVE
                && !now.isBefore(promotion.getStartsAt())
                && !now.isAfter(promotion.getEndsAt());
        String reason = valid ? null
                : (promotion.getStatus() != PromotionStatus.ACTIVE
                        ? "Voucher is not active" : "Voucher is not currently valid");
        return new VoucherInfoResponse(
                promotion.getCode(),
                promotion.getName(),
                promotion.getDiscountType().name(),
                promotion.getDiscountValue(),
                promotion.getMinOrderValue(),
                promotion.getEndsAt(),
                valid,
                reason);
    }

    @Override
    public List<MyVoucherResponse> myVouchers(UUID userId, boolean usableOnly) {
        List<MyVoucherResponse> result = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        for (VoucherAssignment assignment : assignmentRepository.findByUserId(userId)) {
            Promotion promotion = promotionRepository.findById(assignment.getPromotionId()).orElse(null);
            if (promotion == null) {
                continue;
            }
            long usedCount = usageRepository.countByPromotionIdAndUserIdAndStatus(
                    promotion.getId(), userId, UsageStatus.APPLIED);
            int limit = promotion.getPerUserLimit() == null ? 1 : promotion.getPerUserLimit();
            boolean usable = promotion.getStatus() == PromotionStatus.ACTIVE
                    && !now.isAfter(promotion.getEndsAt())
                    && usedCount < limit;
            if (usableOnly && !usable) {
                continue;
            }
            result.add(new MyVoucherResponse(
                    promotion.getId(), promotion.getCode(), promotion.getName(),
                    promotion.getDiscountValue(), promotion.getEndsAt(), usedCount, limit));
        }
        return result;
    }

    @Override
    @Transactional
    public void claim(UUID userId, String code) {
        Promotion promotion = promotionRepository.findByCode(code.trim().toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Voucher code not found"));
        if (promotion.getType() != PromotionType.VOUCHER) {
            throw new BadRequestException("Code is not a voucher");
        }
        if (promotion.getStatus() != PromotionStatus.ACTIVE) {
            throw new BadRequestException("Voucher is not active");
        }
        if (assignmentRepository.existsByPromotionIdAndUserId(promotion.getId(), userId)) {
            throw new ConflictException("Voucher already claimed");
        }
        assignmentRepository.save(VoucherAssignment.builder()
                .promotionId(promotion.getId())
                .userId(userId)
                .source("CLAIM")
                .build());
    }

    @Override
    @Transactional
    public int assignBulk(AssignBulkRequest request) {
        Promotion promotion = promotionRepository.findById(request.promotionId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Promotion not found: " + request.promotionId()));
        String source = request.source() == null || request.source().isBlank()
                ? "MANUAL" : request.source();
        int assigned = 0;
        for (UUID userId : request.userIds()) {
            if (!assignmentRepository.existsByPromotionIdAndUserId(promotion.getId(), userId)) {
                assignmentRepository.save(VoucherAssignment.builder()
                        .promotionId(promotion.getId())
                        .userId(userId)
                        .source(source)
                        .build());
                assigned++;
            }
        }
        return assigned;
    }

    @Override
    @Transactional
    public void assignSignupBonus(UUID userId) {
        String code = properties.signupBonusCode();
        if (code == null || code.isBlank()) {
            return;
        }
        Promotion promotion = promotionRepository.findByCode(code.trim().toUpperCase()).orElse(null);
        if (promotion == null || promotion.getType() != PromotionType.VOUCHER) {
            log.warn("Signup bonus code '{}' is not a usable voucher, skipping", code);
            return;
        }
        if (assignmentRepository.existsByPromotionIdAndUserId(promotion.getId(), userId)) {
            return;
        }
        assignmentRepository.save(VoucherAssignment.builder()
                .promotionId(promotion.getId())
                .userId(userId)
                .source("SIGNUP_BONUS")
                .build());
        log.info("Assigned signup bonus voucher {} to user {}", code, userId);
    }
}
