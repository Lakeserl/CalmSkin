package com.lakeserl.promotion_service.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.lakeserl.promotion_service.config.properties.PromotionProperties;
import com.lakeserl.promotion_service.dto.response.ApiResponse;
import com.lakeserl.promotion_service.dto.response.MyVoucherResponse;
import com.lakeserl.promotion_service.exception.TooManyRequestsException;
import com.lakeserl.promotion_service.service.RateLimitService;
import com.lakeserl.promotion_service.service.VoucherService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/** Authenticated voucher wallet endpoints. */
@RestController
@RequestMapping("/api/v1/promotions/vouchers")
@RequiredArgsConstructor
@Tag(name = "Vouchers", description = "User voucher wallet")
public class VoucherController {

    private final VoucherService voucherService;
    private final RateLimitService rateLimitService;
    private final PromotionProperties properties;

    @GetMapping("/me")
    @Operation(summary = "List the caller's vouchers")
    public ApiResponse<List<MyVoucherResponse>> myVouchers(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestParam(defaultValue = "false") boolean usable) {
        return ApiResponse.ok(voucherService.myVouchers(userId, usable));
    }

    @PostMapping("/claim/{code}")
    @Operation(summary = "Claim a public voucher code")
    public ApiResponse<Void> claim(@RequestHeader("X-User-Id") UUID userId,
                                   @PathVariable String code) {
        if (!rateLimitService.voucherClaimAllowed(userId.toString(), properties.voucherClaimRatePerMinute())) {
            throw new TooManyRequestsException("Too many claim attempts, please slow down");
        }
        voucherService.claim(userId, code);
        return ApiResponse.ok("Voucher claimed", null);
    }
}
