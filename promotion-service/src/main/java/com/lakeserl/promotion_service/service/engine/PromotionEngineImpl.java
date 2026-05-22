package com.lakeserl.promotion_service.service.engine;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Service;

import com.lakeserl.promotion_service.dto.request.CartItemDto;
import com.lakeserl.promotion_service.entity.FlashSale;
import com.lakeserl.promotion_service.entity.Promotion;
import com.lakeserl.promotion_service.entity.PromotionGift;
import com.lakeserl.promotion_service.enums.PromotionStatus;
import com.lakeserl.promotion_service.enums.PromotionType;
import com.lakeserl.promotion_service.repository.FlashSaleRepository;
import com.lakeserl.promotion_service.repository.PromotionGiftRepository;
import com.lakeserl.promotion_service.repository.PromotionRepository;
import com.lakeserl.promotion_service.service.engine.DiscountResult.AppliedPromotion;
import com.lakeserl.promotion_service.service.engine.DiscountResult.FlashSalePriceLine;
import com.lakeserl.promotion_service.service.engine.DiscountResult.GiftLine;

import lombok.RequiredArgsConstructor;

/**
 * Default promotion engine. Gathers voucher + automatic promotions, evaluates
 * each, resolves stacking (all-stackable merge, otherwise the best
 * non-stackable wins), and computes flash-sale prices as a separate pricing
 * layer (it never replaces inventory).
 */
@Service
@RequiredArgsConstructor
public class PromotionEngineImpl implements PromotionEngine {

    private final PromotionRepository promotionRepository;
    private final FlashSaleRepository flashSaleRepository;
    private final PromotionGiftRepository promotionGiftRepository;
    private final PromotionEvaluator evaluator;

    @Override
    public DiscountResult calculate(CartContext context) {
        List<String> warnings = new ArrayList<>();
        Map<Long, Promotion> candidates = collectCandidates(context, warnings);

        List<EvaluatedPromotion> eligible = new ArrayList<>();
        for (Promotion promotion : candidates.values()) {
            EvaluatedPromotion evaluated = evaluator.evaluate(promotion, context);
            if (evaluated.eligible()) {
                eligible.add(evaluated);
            } else {
                warnings.add(label(promotion) + ": " + evaluated.reason());
            }
        }

        List<EvaluatedPromotion> chosen = resolveStacking(eligible);

        BigDecimal totalDiscount = BigDecimal.ZERO;
        BigDecimal shippingDiscount = BigDecimal.ZERO;
        List<AppliedPromotion> applied = new ArrayList<>();
        for (EvaluatedPromotion ev : chosen) {
            totalDiscount = totalDiscount.add(ev.discount());
            shippingDiscount = shippingDiscount.add(ev.shippingDiscount());
            Promotion p = ev.promotion();
            applied.add(new AppliedPromotion(p.getId(), p.getName(),
                    ev.totalBenefit(), p.getDiscountType(), p.getType()));
        }
        totalDiscount = totalDiscount.min(context.safeCartTotal());
        shippingDiscount = shippingDiscount.min(context.safeShippingFee());

        return new DiscountResult(applied, totalDiscount, shippingDiscount,
                computeFlashSalePrices(context), computeFreeGifts(chosen), warnings);
    }

    private Map<Long, Promotion> collectCandidates(CartContext context, List<String> warnings) {
        Map<Long, Promotion> candidates = new LinkedHashMap<>();
        if (context.voucherCodes() != null) {
            for (String rawCode : context.voucherCodes()) {
                if (rawCode == null || rawCode.isBlank()) {
                    continue;
                }
                String code = rawCode.trim().toUpperCase();
                Promotion promotion = promotionRepository.findByCode(code).orElse(null);
                if (promotion == null) {
                    warnings.add(code + ": voucher code not found");
                } else if (promotion.getStatus() != PromotionStatus.ACTIVE) {
                    warnings.add(code + ": voucher is not active");
                } else {
                    candidates.putIfAbsent(promotion.getId(), promotion);
                }
            }
        }
        for (Promotion promotion : promotionRepository.findByStatus(PromotionStatus.ACTIVE)) {
            if (promotion.getType() == PromotionType.PRODUCT_DISCOUNT
                    || promotion.getType() == PromotionType.CATEGORY_DISCOUNT
                    || promotion.getType() == PromotionType.FREE_GIFT) {
                candidates.putIfAbsent(promotion.getId(), promotion);
            }
        }
        return candidates;
    }

    /** Gift products granted by the FREE_GIFT promotions that passed evaluation. */
    private List<GiftLine> computeFreeGifts(List<EvaluatedPromotion> chosen) {
        List<Long> giftPromotionIds = chosen.stream()
                .map(EvaluatedPromotion::promotion)
                .filter(p -> p.getType() == PromotionType.FREE_GIFT)
                .map(Promotion::getId)
                .toList();
        if (giftPromotionIds.isEmpty()) {
            return List.of();
        }
        List<GiftLine> gifts = new ArrayList<>();
        for (PromotionGift gift : promotionGiftRepository.findByPromotionIdIn(giftPromotionIds)) {
            gifts.add(new GiftLine(gift.getProductId(), gift.getVariantId(), gift.getQuantity()));
        }
        return gifts;
    }

    /**
     * If every eligible promotion is stackable they all apply; if any is
     * non-stackable, the single best-value non-stackable one is kept alongside
     * the stackable ones.
     */
    private List<EvaluatedPromotion> resolveStacking(List<EvaluatedPromotion> eligible) {
        List<EvaluatedPromotion> chosen = new ArrayList<>();
        EvaluatedPromotion bestNonStackable = null;
        for (EvaluatedPromotion ev : eligible) {
            if (Boolean.TRUE.equals(ev.promotion().getIsStackable())) {
                chosen.add(ev);
            } else if (bestNonStackable == null
                    || ev.totalBenefit().compareTo(bestNonStackable.totalBenefit()) > 0) {
                bestNonStackable = ev;
            }
        }
        if (bestNonStackable != null) {
            chosen.add(bestNonStackable);
        }
        chosen.sort(Comparator.comparingInt(
                (EvaluatedPromotion ev) -> ev.promotion().getPriority() == null
                        ? 0 : ev.promotion().getPriority()).reversed());
        return chosen;
    }

    /** Flash-sale price for each cart item that has an active, available flash sale. */
    private List<FlashSalePriceLine> computeFlashSalePrices(CartContext context) {
        List<FlashSalePriceLine> lines = new ArrayList<>();
        if (!context.hasCartItems()) {
            return lines;
        }
        List<Long> flashPromotionIds = promotionRepository.findByStatus(PromotionStatus.ACTIVE).stream()
                .filter(p -> p.getType() == PromotionType.FLASH_SALE)
                .map(Promotion::getId)
                .toList();
        if (flashPromotionIds.isEmpty()) {
            return lines;
        }
        List<FlashSale> sales = flashSaleRepository.findByPromotionIdIn(flashPromotionIds);
        for (CartItemDto item : context.cartItems()) {
            for (FlashSale sale : sales) {
                if (sale.getProductId().equals(item.productId())
                        && Objects.equals(sale.getVariantId(), item.variantId())
                        && sale.available() > 0) {
                    lines.add(new FlashSalePriceLine(sale.getProductId(), sale.getVariantId(),
                            sale.getOriginalPrice(), sale.getSalePrice()));
                    break;
                }
            }
        }
        return lines;
    }

    private String label(Promotion promotion) {
        return promotion.getCode() != null ? promotion.getCode() : promotion.getName();
    }
}
