package com.lakeserl.promotion_service.service;

import java.util.List;

import com.lakeserl.promotion_service.dto.response.FlashSaleAvailabilityResponse;
import com.lakeserl.promotion_service.dto.response.FlashSaleSlotsResponse;
import com.lakeserl.promotion_service.dto.response.FlashSaleView;

/** Read access to flash-sale promotions and their slot availability. */
public interface FlashSaleService {

    List<FlashSaleView> currentFlashSales();

    List<FlashSaleView> upcomingFlashSales();

    FlashSaleAvailabilityResponse availability(Long productId, Long variantId);

    FlashSaleSlotsResponse slots(Long flashSaleId);
}
