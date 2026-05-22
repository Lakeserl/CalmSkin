package com.lakeserl.promotion_service.service;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lakeserl.promotion_service.config.properties.PromotionProperties;
import com.lakeserl.promotion_service.dto.request.AssignBulkRequest;
import com.lakeserl.promotion_service.dto.request.GenerateVoucherCodesRequest;
import com.lakeserl.promotion_service.dto.request.VoucherValidationRequest;
import com.lakeserl.promotion_service.dto.response.MyVoucherResponse;
import com.lakeserl.promotion_service.dto.response.VoucherCodeResponse;
import com.lakeserl.promotion_service.dto.response.VoucherInfoResponse;
import com.lakeserl.promotion_service.dto.response.VoucherValidationResponse;
import com.lakeserl.promotion_service.entity.Promotion;
import com.lakeserl.promotion_service.entity.VoucherAssignment;
import com.lakeserl.promotion_service.entity.VoucherCode;
import com.lakeserl.promotion_service.enums.PromotionStatus;
import com.lakeserl.promotion_service.enums.PromotionType;
import com.lakeserl.promotion_service.enums.UsageStatus;
import com.lakeserl.promotion_service.enums.VoucherCodeStatus;
import com.lakeserl.promotion_service.exception.BadRequestException;
import com.lakeserl.promotion_service.exception.ConflictException;
import com.lakeserl.promotion_service.exception.ForbiddenException;
import com.lakeserl.promotion_service.exception.ResourceNotFoundException;
import com.lakeserl.promotion_service.repository.PromotionRepository;
import com.lakeserl.promotion_service.repository.PromotionUsageRepository;
import com.lakeserl.promotion_service.repository.VoucherAssignmentRepository;
import com.lakeserl.promotion_service.repository.VoucherCodeRepository;
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

    private static final char[] CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int CODE_LENGTH = 12;
    private static final int MAX_CODES_PER_BATCH = 10_000;

    private final PromotionRepository promotionRepository;
    private final VoucherAssignmentRepository assignmentRepository;
    private final VoucherCodeRepository voucherCodeRepository;
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

    /**
     * Claims a code into the caller's voucher wallet. A code may be either a
     * shared promotion code or a single-use campaign code; campaign codes are
     * consumed (status -> USED) so they cannot be claimed twice.
     */
    @Override
    @Transactional
    public void claim(UUID userId, String rawCode) {
        String code = rawCode.trim().toUpperCase();
        Promotion shared = promotionRepository.findByCode(code).orElse(null);
        if (shared != null) {
            claimSharedVoucher(userId, shared);
            return;
        }
        VoucherCode campaignCode = voucherCodeRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Voucher code not found"));
        claimCampaignCode(userId, campaignCode);
    }

    private void claimSharedVoucher(UUID userId, Promotion promotion) {
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

    private void claimCampaignCode(UUID userId, VoucherCode campaignCode) {
        if (campaignCode.getStatus() != VoucherCodeStatus.ACTIVE) {
            throw new ConflictException("Voucher code is no longer available");
        }
        if (campaignCode.getAssignedUserId() != null
                && !campaignCode.getAssignedUserId().equals(userId)) {
            throw new ForbiddenException("This voucher code belongs to another user");
        }
        Promotion promotion = promotionRepository.findById(campaignCode.getPromotionId())
                .orElseThrow(() -> new ResourceNotFoundException("Promotion not found"));
        if (promotion.getStatus() == PromotionStatus.EXPIRED
                || promotion.getStatus() == PromotionStatus.CANCELLED) {
            throw new BadRequestException("Voucher is no longer available");
        }
        if (assignmentRepository.existsByPromotionIdAndUserId(promotion.getId(), userId)) {
            throw new ConflictException("Voucher already claimed");
        }
        campaignCode.setStatus(VoucherCodeStatus.USED);
        campaignCode.setAssignedUserId(userId);
        campaignCode.setClaimedAt(LocalDateTime.now());
        voucherCodeRepository.save(campaignCode);
        assignmentRepository.save(VoucherAssignment.builder()
                .promotionId(promotion.getId())
                .userId(userId)
                .source("CODE")
                .build());
        log.info("User {} claimed campaign code for promotion {}", userId, promotion.getId());
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

    @Override
    @Transactional
    public List<VoucherCodeResponse> generateCodes(Long promotionId, GenerateVoucherCodesRequest request) {
        Promotion promotion = promotionRepository.findById(promotionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Promotion not found: " + promotionId));
        List<UUID> boundUsers = request.assignedUserIds();
        boolean bound = boundUsers != null && !boundUsers.isEmpty();
        int count = bound ? boundUsers.size()
                : (request.count() == null ? 0 : request.count());
        if (count <= 0) {
            throw new BadRequestException(
                    "Provide a positive count or a non-empty assignedUserIds list");
        }
        if (count > MAX_CODES_PER_BATCH) {
            throw new BadRequestException(
                    "Cannot generate more than " + MAX_CODES_PER_BATCH + " codes at once");
        }
        List<VoucherCodeResponse> generated = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            String code = uniqueCode();
            UUID assignee = bound ? boundUsers.get(i) : null;
            voucherCodeRepository.save(VoucherCode.builder()
                    .promotionId(promotion.getId())
                    .code(code)
                    .assignedUserId(assignee)
                    .status(VoucherCodeStatus.ACTIVE)
                    .build());
            generated.add(new VoucherCodeResponse(code, assignee, VoucherCodeStatus.ACTIVE.name()));
        }
        log.info("Generated {} voucher codes for promotion {}", count, promotionId);
        return generated;
    }

    @Override
    public List<VoucherCodeResponse> listCodes(Long promotionId) {
        return voucherCodeRepository.findByPromotionId(promotionId).stream()
                .map(vc -> new VoucherCodeResponse(
                        vc.getCode(), vc.getAssignedUserId(), vc.getStatus().name()))
                .toList();
    }

    @Override
    @Transactional
    public int grantByCode(String code, Collection<UUID> userIds, String source) {
        if (code == null || code.isBlank() || userIds == null || userIds.isEmpty()) {
            return 0;
        }
        Promotion promotion = promotionRepository.findByCode(code.trim().toUpperCase()).orElse(null);
        if (promotion == null || promotion.getType() != PromotionType.VOUCHER) {
            log.warn("grantByCode: '{}' is not a usable voucher, skipping", code);
            return 0;
        }
        int granted = 0;
        for (UUID userId : userIds) {
            if (!assignmentRepository.existsByPromotionIdAndUserId(promotion.getId(), userId)) {
                assignmentRepository.save(VoucherAssignment.builder()
                        .promotionId(promotion.getId())
                        .userId(userId)
                        .source(source)
                        .build());
                granted++;
            }
        }
        return granted;
    }

    /** Draws a random code, retrying on the rare collision with an existing one. */
    private String uniqueCode() {
        for (int attempt = 0; attempt < 10; attempt++) {
            String code = randomCode();
            if (!voucherCodeRepository.existsByCode(code)) {
                return code;
            }
        }
        throw new ConflictException("Could not generate a unique voucher code, please retry");
    }

    private String randomCode() {
        StringBuilder code = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            code.append(CODE_ALPHABET[RANDOM.nextInt(CODE_ALPHABET.length)]);
        }
        return code.toString();
    }
}
