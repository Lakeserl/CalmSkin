package com.lakeserl.promotion_service.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lakeserl.promotion_service.dto.request.CreatePromotionRequest;
import com.lakeserl.promotion_service.dto.request.LockRequest;
import com.lakeserl.promotion_service.dto.request.PreviewRequest;
import com.lakeserl.promotion_service.dto.request.UpdatePromotionRequest;
import com.lakeserl.promotion_service.dto.response.ConfirmResponse;
import com.lakeserl.promotion_service.dto.response.LockResponse;
import com.lakeserl.promotion_service.dto.response.PreviewResponse;
import com.lakeserl.promotion_service.dto.response.PromotionResponse;
import com.lakeserl.promotion_service.dto.response.PromotionStatsResponse;
import com.lakeserl.promotion_service.dto.response.PromotionSummaryResponse;
import com.lakeserl.promotion_service.dto.response.ReleaseResponse;
import com.lakeserl.promotion_service.config.properties.PromotionProperties;
import com.lakeserl.promotion_service.entity.BundleItem;
import com.lakeserl.promotion_service.entity.FlashSale;
import com.lakeserl.promotion_service.entity.FlashSaleReservation;
import com.lakeserl.promotion_service.entity.Promotion;
import com.lakeserl.promotion_service.entity.PromotionTier;
import com.lakeserl.promotion_service.entity.PromotionUsage;
import com.lakeserl.promotion_service.entity.PromotionUsageLock;
import com.lakeserl.promotion_service.enums.PromotionStatus;
import com.lakeserl.promotion_service.enums.PromotionType;
import com.lakeserl.promotion_service.enums.UsageStatus;
import com.lakeserl.promotion_service.event.producer.PromotionEventProducer;
import com.lakeserl.promotion_service.exception.BadRequestException;
import com.lakeserl.promotion_service.exception.ConflictException;
import com.lakeserl.promotion_service.exception.ResourceNotFoundException;
import com.lakeserl.promotion_service.repository.BundleItemRepository;
import com.lakeserl.promotion_service.repository.FlashSaleRepository;
import com.lakeserl.promotion_service.repository.FlashSaleReservationRepository;
import com.lakeserl.promotion_service.repository.PromotionRepository;
import com.lakeserl.promotion_service.repository.PromotionTierRepository;
import com.lakeserl.promotion_service.repository.PromotionUsageLockRepository;
import com.lakeserl.promotion_service.repository.PromotionUsageRepository;
import com.lakeserl.promotion_service.service.engine.CartContext;
import com.lakeserl.promotion_service.service.engine.DiscountResult;
import com.lakeserl.promotion_service.service.engine.EvaluatedPromotion;
import com.lakeserl.promotion_service.service.engine.PromotionEngine;
import com.lakeserl.promotion_service.service.engine.PromotionEvaluator;
import com.lakeserl.promotion_service.support.CsvIds;
import com.lakeserl.promotion_service.support.PromotionMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PromotionServiceImpl implements PromotionService {

    private final PromotionRepository promotionRepository;
    private final PromotionTierRepository tierRepository;
    private final FlashSaleRepository flashSaleRepository;
    private final FlashSaleReservationRepository flashSaleReservationRepository;
    private final BundleItemRepository bundleItemRepository;
    private final PromotionUsageRepository usageRepository;
    private final PromotionUsageLockRepository lockRepository;
    private final PromotionEngine promotionEngine;
    private final PromotionEvaluator evaluator;
    private final PromotionEventProducer eventProducer;
    private final PromotionProperties properties;

    // ---- Admin management -------------------------------------------------

    @Override
    @Transactional
    public PromotionResponse create(CreatePromotionRequest request, UUID createdBy) {
        if (!request.endsAt().isAfter(request.startsAt())) {
            throw new BadRequestException("endsAt must be after startsAt");
        }
        String code = request.code() == null || request.code().isBlank()
                ? null : request.code().trim().toUpperCase();
        if (code != null && promotionRepository.findByCode(code).isPresent()) {
            throw new ConflictException("Promotion code already exists: " + code);
        }

        Promotion promotion = Promotion.builder()
                .code(code)
                .name(request.name())
                .description(request.description())
                .type(request.type())
                .discountType(request.discountType())
                .discountValue(request.discountValue())
                .maxDiscountAmount(request.maxDiscountAmount())
                .minOrderValue(request.minOrderValue() == null ? BigDecimal.ZERO : request.minOrderValue())
                .minItemQuantity(request.minItemQuantity() == null ? 0 : request.minItemQuantity())
                .applicableProductIds(CsvIds.join(request.applicableProductIds()))
                .applicableCategoryIds(CsvIds.join(request.applicableCategoryIds()))
                .applicableBrandIds(CsvIds.join(request.applicableBrandIds()))
                .excludedProductIds(CsvIds.join(request.excludedProductIds()))
                .totalUsageLimit(request.totalUsageLimit())
                .perUserLimit(request.perUserLimit() == null ? 1 : request.perUserLimit())
                .startsAt(request.startsAt())
                .endsAt(request.endsAt())
                .status(PromotionStatus.DRAFT)
                .isStackable(request.isStackable() != null && request.isStackable())
                .priority(request.priority() == null ? 0 : request.priority())
                .createdBy(createdBy)
                .build();
        promotion = promotionRepository.save(promotion);

        saveChildren(promotion.getId(), request);
        log.info("Created promotion id={} code={}", promotion.getId(), code);
        return PromotionMapper.toResponse(promotion);
    }

    private void saveChildren(Long promotionId, CreatePromotionRequest request) {
        if (request.tiers() != null) {
            for (CreatePromotionRequest.TierInput t : request.tiers()) {
                tierRepository.save(PromotionTier.builder()
                        .promotionId(promotionId)
                        .minQuantity(t.minQuantity())
                        .minValue(t.minValue())
                        .discountType(t.discountType())
                        .discountValue(t.discountValue())
                        .sortOrder(t.sortOrder() == null ? (short) 0 : t.sortOrder())
                        .build());
            }
        }
        if (request.flashSales() != null) {
            for (CreatePromotionRequest.FlashSaleInput f : request.flashSales()) {
                flashSaleRepository.save(FlashSale.builder()
                        .promotionId(promotionId)
                        .productId(f.productId())
                        .variantId(f.variantId())
                        .originalPrice(f.originalPrice())
                        .salePrice(f.salePrice())
                        .quantityLimit(f.quantityLimit())
                        .quantitySold(0)
                        .quantityReserved(0)
                        .build());
            }
        }
        if (request.bundleItems() != null) {
            for (CreatePromotionRequest.BundleInput b : request.bundleItems()) {
                bundleItemRepository.save(BundleItem.builder()
                        .promotionId(promotionId)
                        .productId(b.productId())
                        .variantId(b.variantId())
                        .quantity(b.quantity() == null ? 1 : b.quantity())
                        .build());
            }
        }
    }

    @Override
    @Transactional
    public PromotionResponse update(Long id, UpdatePromotionRequest request) {
        Promotion promotion = require(id);
        if (promotion.getStatus() == PromotionStatus.EXPIRED
                || promotion.getStatus() == PromotionStatus.CANCELLED) {
            throw new BadRequestException("A " + promotion.getStatus() + " promotion cannot be edited");
        }
        if (request.name() != null) {
            promotion.setName(request.name());
        }
        if (request.description() != null) {
            promotion.setDescription(request.description());
        }
        if (request.discountValue() != null) {
            promotion.setDiscountValue(request.discountValue());
        }
        if (request.maxDiscountAmount() != null) {
            promotion.setMaxDiscountAmount(request.maxDiscountAmount());
        }
        if (request.minOrderValue() != null) {
            promotion.setMinOrderValue(request.minOrderValue());
        }
        if (request.minItemQuantity() != null) {
            promotion.setMinItemQuantity(request.minItemQuantity());
        }
        if (request.applicableProductIds() != null) {
            promotion.setApplicableProductIds(CsvIds.join(request.applicableProductIds()));
        }
        if (request.applicableCategoryIds() != null) {
            promotion.setApplicableCategoryIds(CsvIds.join(request.applicableCategoryIds()));
        }
        if (request.applicableBrandIds() != null) {
            promotion.setApplicableBrandIds(CsvIds.join(request.applicableBrandIds()));
        }
        if (request.excludedProductIds() != null) {
            promotion.setExcludedProductIds(CsvIds.join(request.excludedProductIds()));
        }
        if (request.totalUsageLimit() != null) {
            promotion.setTotalUsageLimit(request.totalUsageLimit());
        }
        if (request.perUserLimit() != null) {
            promotion.setPerUserLimit(request.perUserLimit());
        }
        if (request.startsAt() != null) {
            promotion.setStartsAt(request.startsAt());
        }
        if (request.endsAt() != null) {
            promotion.setEndsAt(request.endsAt());
        }
        if (request.isStackable() != null) {
            promotion.setIsStackable(request.isStackable());
        }
        if (request.priority() != null) {
            promotion.setPriority(request.priority());
        }
        if (!promotion.getEndsAt().isAfter(promotion.getStartsAt())) {
            throw new BadRequestException("endsAt must be after startsAt");
        }
        return PromotionMapper.toResponse(promotionRepository.save(promotion));
    }

    @Override
    @Transactional
    public PromotionResponse updateStatus(Long id, PromotionStatus status) {
        if (status != PromotionStatus.SCHEDULED
                && status != PromotionStatus.ACTIVE
                && status != PromotionStatus.PAUSED
                && status != PromotionStatus.CANCELLED) {
            throw new BadRequestException(
                    "Status can only be set to SCHEDULED, ACTIVE, PAUSED or CANCELLED");
        }
        Promotion promotion = require(id);
        promotion.setStatus(status);
        return PromotionMapper.toResponse(promotionRepository.save(promotion));
    }

    @Override
    public PromotionResponse getById(Long id) {
        return PromotionMapper.toResponse(require(id));
    }

    @Override
    public Page<PromotionResponse> search(PromotionType type, PromotionStatus status, Pageable pageable) {
        return promotionRepository.search(type, status, pageable).map(PromotionMapper::toResponse);
    }

    @Override
    public List<PromotionSummaryResponse> activePromotions() {
        return PromotionMapper.toSummaries(promotionRepository.findByStatus(PromotionStatus.ACTIVE));
    }

    // ---- Order-facing apply flow -----------------------------------------

    @Override
    public PreviewResponse preview(PreviewRequest request) {
        CartContext context = new CartContext(
                request.userId(), request.cartItems(), request.voucherCodes(),
                request.cartTotal(), request.shippingFee());
        DiscountResult result = promotionEngine.calculate(context);

        List<PreviewResponse.ApplicablePromotion> applicable = result.appliedPromotions().stream()
                .map(ap -> new PreviewResponse.ApplicablePromotion(
                        ap.promotionId(), ap.name(), ap.discountAmount(),
                        ap.discountType().name(), ap.type().name()))
                .toList();
        List<PreviewResponse.FlashSalePrice> flashPrices = result.flashSalePrices().stream()
                .map(fp -> new PreviewResponse.FlashSalePrice(
                        fp.productId(), fp.variantId(), fp.originalPrice(), fp.salePrice()))
                .toList();

        BigDecimal cartTotal = context.safeCartTotal();
        BigDecimal shippingFee = context.safeShippingFee();
        BigDecimal discountedShipping = shippingFee.subtract(result.shippingDiscount()).max(BigDecimal.ZERO);
        BigDecimal finalTotal = cartTotal.subtract(result.totalDiscount()).max(BigDecimal.ZERO)
                .add(discountedShipping);

        return new PreviewResponse(applicable, flashPrices, result.totalDiscount(),
                discountedShipping, finalTotal, result.warnings());
    }

    @Override
    @Transactional
    public LockResponse lock(LockRequest request) {
        List<Long> lockedIds = new ArrayList<>();
        BigDecimal totalDiscount = BigDecimal.ZERO;

        Map<Long, PromotionUsageLock> existing = new LinkedHashMap<>();
        for (PromotionUsageLock lock : lockRepository.findByOrderId(request.orderId())) {
            existing.put(lock.getPromotionId(), lock);
        }

        for (Long promotionId : request.promotionIds()) {
            PromotionUsageLock current = existing.get(promotionId);
            if (current != null) {
                // Idempotent re-lock for the same order: keep the existing lock.
                lockedIds.add(promotionId);
                totalDiscount = totalDiscount.add(current.getDiscountAmount());
                continue;
            }
            Promotion promotion = require(promotionId);
            CartContext context = new CartContext(
                    request.userId(), request.cartItems(), null, request.cartTotal(), BigDecimal.ZERO);
            EvaluatedPromotion evaluated = evaluator.evaluate(promotion, context);
            if (!evaluated.eligible()) {
                throw new ConflictException(
                        "Cannot lock " + label(promotion) + ": " + evaluated.reason());
            }
            try {
                lockRepository.save(PromotionUsageLock.builder()
                        .promotionId(promotionId)
                        .userId(request.userId())
                        .orderId(request.orderId())
                        .discountAmount(evaluated.totalBenefit())
                        .expiresAt(LocalDateTime.now().plusMinutes(properties.lockTtlMinutes()))
                        .build());
            } catch (DataIntegrityViolationException ex) {
                throw new ConflictException(
                        "Promotion " + label(promotion) + " is already locked for this user");
            }
            if (promotion.getType() == PromotionType.FLASH_SALE) {
                reserveFlashSlots(promotion, request);
            }
            lockedIds.add(promotionId);
            totalDiscount = totalDiscount.add(evaluated.totalBenefit());
        }
        return new LockResponse(true, lockedIds, totalDiscount);
    }

    /**
     * Holds flash-sale units for the order. Each line is loaded under a
     * pessimistic write lock so concurrent orders cannot oversell.
     */
    private void reserveFlashSlots(Promotion promotion, LockRequest request) {
        List<FlashSale> lines = flashSaleRepository.findByPromotionId(promotion.getId());
        if (lines.isEmpty()) {
            return;
        }
        if (request.cartItems() == null || request.cartItems().isEmpty()) {
            throw new BadRequestException(
                    "Cart items are required to lock flash-sale promotion " + label(promotion));
        }
        for (FlashSale line : lines) {
            int requested = request.cartItems().stream()
                    .filter(ci -> line.getProductId().equals(ci.productId())
                            && Objects.equals(line.getVariantId(), ci.variantId()))
                    .mapToInt(ci -> ci.quantity() == null ? 0 : ci.quantity())
                    .sum();
            if (requested <= 0) {
                continue;
            }
            FlashSale locked = flashSaleRepository.lockById(line.getId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Flash sale not found: " + line.getId()));
            if (locked.available() < requested) {
                throw new ConflictException(
                        "Flash sale sold out for product " + locked.getProductId());
            }
            locked.setQuantityReserved(locked.getQuantityReserved() + requested);
            flashSaleReservationRepository.save(FlashSaleReservation.builder()
                    .flashSaleId(locked.getId())
                    .orderId(request.orderId())
                    .quantity(requested)
                    .build());
        }
    }

    /** Converts an order's flash reservations into confirmed sales. */
    private void finalizeFlashReservations(String orderId) {
        for (FlashSaleReservation reservation : flashSaleReservationRepository.findByOrderId(orderId)) {
            FlashSale sale = flashSaleRepository.lockById(reservation.getFlashSaleId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Flash sale not found: " + reservation.getFlashSaleId()));
            sale.setQuantitySold(sale.getQuantitySold() + reservation.getQuantity());
            sale.setQuantityReserved(
                    Math.max(0, sale.getQuantityReserved() - reservation.getQuantity()));
            flashSaleReservationRepository.delete(reservation);
        }
    }

    /** Returns an order's held flash-sale units back to the available pool. */
    private void restoreFlashReservations(String orderId) {
        for (FlashSaleReservation reservation : flashSaleReservationRepository.findByOrderId(orderId)) {
            FlashSale sale = flashSaleRepository.lockById(reservation.getFlashSaleId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Flash sale not found: " + reservation.getFlashSaleId()));
            sale.setQuantityReserved(
                    Math.max(0, sale.getQuantityReserved() - reservation.getQuantity()));
            flashSaleReservationRepository.delete(reservation);
        }
    }

    @Override
    @Transactional
    public ConfirmResponse confirm(String orderId) {
        List<PromotionUsageLock> locks = lockRepository.findByOrderId(orderId);
        if (locks.isEmpty()) {
            return new ConfirmResponse(true);
        }
        List<Long> promotionIds = new ArrayList<>();
        BigDecimal totalDiscount = BigDecimal.ZERO;
        UUID userId = locks.get(0).getUserId();
        for (PromotionUsageLock lock : locks) {
            if (!usageRepository.existsByOrderIdAndPromotionId(orderId, lock.getPromotionId())) {
                usageRepository.save(PromotionUsage.builder()
                        .promotionId(lock.getPromotionId())
                        .userId(lock.getUserId())
                        .orderId(orderId)
                        .discountAmount(lock.getDiscountAmount())
                        .status(UsageStatus.APPLIED)
                        .build());
            }
            promotionIds.add(lock.getPromotionId());
            totalDiscount = totalDiscount.add(lock.getDiscountAmount());
        }
        finalizeFlashReservations(orderId);
        lockRepository.deleteAll(locks);

        eventProducer.publish("promotion.applied", orderId, Map.of(
                "orderId", orderId,
                "userId", userId.toString(),
                "promotionIds", promotionIds,
                "totalDiscount", totalDiscount));
        log.info("Confirmed {} promotions for order {}", promotionIds.size(), orderId);
        return new ConfirmResponse(true);
    }

    @Override
    @Transactional
    public ReleaseResponse release(String orderId) {
        List<PromotionUsageLock> locks = lockRepository.findByOrderId(orderId);
        if (!locks.isEmpty()) {
            lockRepository.deleteAll(locks);
        }
        restoreFlashReservations(orderId);
        for (PromotionUsage usage : usageRepository.findByOrderId(orderId)) {
            if (usage.getStatus() == UsageStatus.APPLIED) {
                usage.setStatus(UsageStatus.ROLLED_BACK);
                usageRepository.save(usage);
            }
        }
        log.info("Released promotions for order {}", orderId);
        return new ReleaseResponse(true);
    }

    // ---- Analytics --------------------------------------------------------

    @Override
    public PromotionStatsResponse stats(Long promotionId) {
        require(promotionId);
        long count = usageRepository.countByPromotionIdAndStatus(promotionId, UsageStatus.APPLIED);
        BigDecimal total = usageRepository.sumDiscountByPromotionIdAndStatus(
                promotionId, UsageStatus.APPLIED);
        if (total == null) {
            total = BigDecimal.ZERO;
        }
        BigDecimal average = count > 0
                ? total.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        return new PromotionStatsResponse(count, total, average);
    }

    @Override
    public Page<PromotionUsage> usages(Long promotionId, Pageable pageable) {
        require(promotionId);
        return usageRepository.findByPromotionId(promotionId, pageable);
    }

    // ---- Helpers ----------------------------------------------------------

    private Promotion require(Long id) {
        Optional<Promotion> promotion = promotionRepository.findById(id);
        return promotion.orElseThrow(() -> new ResourceNotFoundException("Promotion not found: " + id));
    }

    private String label(Promotion promotion) {
        return promotion.getCode() != null ? promotion.getCode() : promotion.getName();
    }
}
