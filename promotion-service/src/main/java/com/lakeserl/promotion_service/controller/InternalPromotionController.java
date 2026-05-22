package com.lakeserl.promotion_service.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.lakeserl.promotion_service.dto.request.LockRequest;
import com.lakeserl.promotion_service.dto.request.OrderRefRequest;
import com.lakeserl.promotion_service.dto.request.PreviewRequest;
import com.lakeserl.promotion_service.dto.request.VoucherValidationRequest;
import com.lakeserl.promotion_service.dto.response.ConfirmResponse;
import com.lakeserl.promotion_service.dto.response.FlashSaleAvailabilityResponse;
import com.lakeserl.promotion_service.dto.response.LockResponse;
import com.lakeserl.promotion_service.dto.response.PreviewResponse;
import com.lakeserl.promotion_service.dto.response.ReleaseResponse;
import com.lakeserl.promotion_service.dto.response.VoucherValidationResponse;
import com.lakeserl.promotion_service.service.FlashSaleService;
import com.lakeserl.promotion_service.service.PromotionService;
import com.lakeserl.promotion_service.service.VoucherService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Service-to-service endpoints called by order-service. Guarded by
 * InternalApiSecurityFilter. These return the raw DTO (no ApiResponse
 * envelope) so the calling client maps the response directly.
 */
@RestController
@RequestMapping("/internal/promotions")
@RequiredArgsConstructor
@Tag(name = "Internal promotions", description = "order-service integration")
public class InternalPromotionController {

    private final PromotionService promotionService;
    private final VoucherService voucherService;
    private final FlashSaleService flashSaleService;

    @PostMapping("/validate")
    @Operation(summary = "Validate a voucher code against an order subtotal")
    public VoucherValidationResponse validate(@RequestBody VoucherValidationRequest request) {
        return voucherService.validate(request);
    }

    @PostMapping("/preview")
    @Operation(summary = "Preview all promotions for a cart (read-only)")
    public PreviewResponse preview(@Valid @RequestBody PreviewRequest request) {
        return promotionService.preview(request);
    }

    @PostMapping("/lock")
    @Operation(summary = "Lock promotions for an order being created")
    public LockResponse lock(@Valid @RequestBody LockRequest request) {
        return promotionService.lock(request);
    }

    @PostMapping("/confirm")
    @Operation(summary = "Confirm an order's locked promotions")
    public ConfirmResponse confirm(@Valid @RequestBody OrderRefRequest request) {
        return promotionService.confirm(request.orderId());
    }

    @PostMapping("/release")
    @Operation(summary = "Release an order's locked promotions")
    public ReleaseResponse release(@Valid @RequestBody OrderRefRequest request) {
        return promotionService.release(request.orderId());
    }

    @GetMapping("/flash-sales/availability")
    @Operation(summary = "Flash-sale availability for a product / variant")
    public FlashSaleAvailabilityResponse flashSaleAvailability(
            @RequestParam Long productId,
            @RequestParam(required = false) Long variantId) {
        return flashSaleService.availability(productId, variantId);
    }
}
