package com.lakeserl.promotion_service.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.lakeserl.promotion_service.config.properties.PromotionProperties;
import com.lakeserl.promotion_service.dto.response.ApiResponse;
import com.lakeserl.promotion_service.dto.response.PromotionSummaryResponse;
import com.lakeserl.promotion_service.dto.response.VoucherInfoResponse;
import com.lakeserl.promotion_service.exception.TooManyRequestsException;
import com.lakeserl.promotion_service.service.PromotionService;
import com.lakeserl.promotion_service.service.RateLimitService;
import com.lakeserl.promotion_service.service.VoucherService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

/** Public promotion browsing. */
@RestController
@RequestMapping("/api/v1/promotions")
@RequiredArgsConstructor
@Tag(name = "Promotions", description = "Public promotion browsing")
public class PromotionController {

    private final PromotionService promotionService;
    private final VoucherService voucherService;
    private final RateLimitService rateLimitService;
    private final PromotionProperties properties;

    @GetMapping("/active")
    @Operation(summary = "List currently active promotions")
    public ApiResponse<List<PromotionSummaryResponse>> active() {
        return ApiResponse.ok(promotionService.activePromotions());
    }

    @GetMapping("/{code}/info")
    @Operation(summary = "Preview a voucher code (no apply, no lock)")
    public ApiResponse<VoucherInfoResponse> codeInfo(@PathVariable String code,
                                                     HttpServletRequest request) {
        if (!rateLimitService.codeInfoAllowed(request.getRemoteAddr(), properties.codeInfoRatePerMinute())) {
            throw new TooManyRequestsException("Too many voucher lookups, please slow down");
        }
        return ApiResponse.ok(voucherService.codeInfo(code));
    }
}
