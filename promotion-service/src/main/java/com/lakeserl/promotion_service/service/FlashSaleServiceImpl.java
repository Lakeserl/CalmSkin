package com.lakeserl.promotion_service.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lakeserl.promotion_service.dto.response.FlashSaleAvailabilityResponse;
import com.lakeserl.promotion_service.dto.response.FlashSaleSlotsResponse;
import com.lakeserl.promotion_service.dto.response.FlashSaleView;
import com.lakeserl.promotion_service.entity.FlashSale;
import com.lakeserl.promotion_service.entity.Promotion;
import com.lakeserl.promotion_service.enums.PromotionStatus;
import com.lakeserl.promotion_service.enums.PromotionType;
import com.lakeserl.promotion_service.exception.ResourceNotFoundException;
import com.lakeserl.promotion_service.repository.FlashSaleRepository;
import com.lakeserl.promotion_service.repository.PromotionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FlashSaleServiceImpl implements FlashSaleService {

    private final PromotionRepository promotionRepository;
    private final FlashSaleRepository flashSaleRepository;

    @Override
    public List<FlashSaleView> currentFlashSales() {
        return viewsForStatus(PromotionStatus.ACTIVE);
    }

    @Override
    public List<FlashSaleView> upcomingFlashSales() {
        return viewsForStatus(PromotionStatus.SCHEDULED);
    }

    @Override
    public FlashSaleAvailabilityResponse availability(Long productId, Long variantId) {
        FlashSale sale = flashSaleRepository.findForProduct(productId, variantId).orElse(null);
        if (sale == null) {
            return FlashSaleAvailabilityResponse.none();
        }
        Promotion promotion = promotionRepository.findById(sale.getPromotionId()).orElse(null);
        if (promotion == null || promotion.getStatus() != PromotionStatus.ACTIVE) {
            return FlashSaleAvailabilityResponse.none();
        }
        return new FlashSaleAvailabilityResponse(
                true, sale.getSalePrice(), Math.max(0, sale.available()), promotion.getEndsAt());
    }

    @Override
    public FlashSaleSlotsResponse slots(Long flashSaleId) {
        FlashSale sale = flashSaleRepository.findById(flashSaleId)
                .orElseThrow(() -> new ResourceNotFoundException("Flash sale not found: " + flashSaleId));
        return new FlashSaleSlotsResponse(
                sale.getId(), sale.getProductId(), sale.getVariantId(),
                sale.getQuantityLimit(), sale.getQuantitySold(),
                sale.getQuantityReserved(), Math.max(0, sale.available()));
    }

    private List<FlashSaleView> viewsForStatus(PromotionStatus status) {
        List<FlashSaleView> views = new ArrayList<>();
        for (Promotion promotion : promotionRepository.findByStatus(status)) {
            if (promotion.getType() != PromotionType.FLASH_SALE) {
                continue;
            }
            List<FlashSale> sales = flashSaleRepository.findByPromotionId(promotion.getId());
            if (sales.isEmpty()) {
                continue;
            }
            List<FlashSaleView.Product> products = sales.stream()
                    .map(s -> new FlashSaleView.Product(
                            s.getProductId(), s.getVariantId(),
                            s.getSalePrice(), s.getOriginalPrice(), Math.max(0, s.available())))
                    .toList();
            views.add(new FlashSaleView(promotion.getId(), promotion.getName(),
                    promotion.getStartsAt(), promotion.getEndsAt(), products));
        }
        return views;
    }
}
