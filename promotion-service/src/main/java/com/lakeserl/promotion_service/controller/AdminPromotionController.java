package com.lakeserl.promotion_service.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.lakeserl.promotion_service.dto.request.AssignBulkRequest;
import com.lakeserl.promotion_service.dto.request.CreatePromotionRequest;
import com.lakeserl.promotion_service.dto.request.GenerateVoucherCodesRequest;
import com.lakeserl.promotion_service.dto.request.UpdatePromotionRequest;
import com.lakeserl.promotion_service.dto.request.UpdateStatusRequest;
import com.lakeserl.promotion_service.dto.response.ApiResponse;
import com.lakeserl.promotion_service.dto.response.FlashSaleSlotsResponse;
import com.lakeserl.promotion_service.dto.response.PromotionResponse;
import com.lakeserl.promotion_service.dto.response.PromotionStatsResponse;
import com.lakeserl.promotion_service.dto.response.VoucherCodeResponse;
import com.lakeserl.promotion_service.entity.PromotionUsage;
import com.lakeserl.promotion_service.enums.PromotionStatus;
import com.lakeserl.promotion_service.enums.PromotionType;
import com.lakeserl.promotion_service.service.FlashSaleService;
import com.lakeserl.promotion_service.service.PromotionService;
import com.lakeserl.promotion_service.service.VoucherService;
import com.lakeserl.promotion_service.support.Pages;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** Admin promotion management. */
@RestController
@RequestMapping("/api/v1/admin/promotions")
@RequiredArgsConstructor
@Tag(name = "Admin promotions", description = "Promotion management and analytics")
public class AdminPromotionController {

    private final PromotionService promotionService;
    private final VoucherService voucherService;
    private final FlashSaleService flashSaleService;

    @PostMapping
    @Operation(summary = "Create a promotion (DRAFT)")
    public ApiResponse<PromotionResponse> create(@RequestHeader("X-User-Id") UUID adminId,
                                                 @Valid @RequestBody CreatePromotionRequest request) {
        return ApiResponse.ok("Promotion created", promotionService.create(request, adminId));
    }

    @GetMapping
    @Operation(summary = "List promotions with optional type/status filter")
    public ApiResponse<List<PromotionResponse>> list(
            @RequestParam(required = false) PromotionType type,
            @RequestParam(required = false) PromotionStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<PromotionResponse> result = promotionService.search(type, status, PageRequest.of(page, size));
        return ApiResponse.ok("Promotions retrieved", result.getContent(), Pages.info(result));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get one promotion")
    public ApiResponse<PromotionResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(promotionService.getById(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a promotion")
    public ApiResponse<PromotionResponse> update(@PathVariable Long id,
                                                 @RequestBody UpdatePromotionRequest request) {
        return ApiResponse.ok("Promotion updated", promotionService.update(id, request));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Change a promotion's status (ACTIVE / PAUSED / CANCELLED)")
    public ApiResponse<PromotionResponse> updateStatus(@PathVariable Long id,
                                                       @Valid @RequestBody UpdateStatusRequest request) {
        return ApiResponse.ok("Status updated", promotionService.updateStatus(id, request.status()));
    }

    @GetMapping("/{id}/usages")
    @Operation(summary = "Paginated usage log for a promotion")
    public ApiResponse<List<PromotionUsage>> usages(@PathVariable Long id,
                                                    @RequestParam(defaultValue = "0") int page,
                                                    @RequestParam(defaultValue = "20") int size) {
        Page<PromotionUsage> result = promotionService.usages(id, PageRequest.of(page, size));
        return ApiResponse.ok("Usages retrieved", result.getContent(), Pages.info(result));
    }

    @GetMapping("/{id}/stats")
    @Operation(summary = "Usage analytics for a promotion")
    public ApiResponse<PromotionStatsResponse> stats(@PathVariable Long id) {
        return ApiResponse.ok(promotionService.stats(id));
    }

    @PostMapping("/vouchers/assign-bulk")
    @Operation(summary = "Assign a voucher to many users at once")
    public ApiResponse<Void> assignBulk(@Valid @RequestBody AssignBulkRequest request) {
        int assigned = voucherService.assignBulk(request);
        return ApiResponse.ok("Voucher assigned to " + assigned + " users", null);
    }

    @PostMapping("/{id}/voucher-codes")
    @Operation(summary = "Bulk-generate single-use campaign voucher codes")
    public ApiResponse<List<VoucherCodeResponse>> generateVoucherCodes(
            @PathVariable Long id,
            @RequestBody GenerateVoucherCodesRequest request) {
        List<VoucherCodeResponse> codes = voucherService.generateCodes(id, request);
        return ApiResponse.ok("Generated " + codes.size() + " voucher codes", codes);
    }

    @GetMapping("/{id}/voucher-codes")
    @Operation(summary = "List campaign voucher codes for a promotion")
    public ApiResponse<List<VoucherCodeResponse>> listVoucherCodes(@PathVariable Long id) {
        return ApiResponse.ok(voucherService.listCodes(id));
    }

    @GetMapping("/flash-sales/{flashSaleId}/slots")
    @Operation(summary = "Real-time slot counters for a flash-sale line")
    public ApiResponse<FlashSaleSlotsResponse> flashSaleSlots(@PathVariable Long flashSaleId) {
        return ApiResponse.ok(flashSaleService.slots(flashSaleId));
    }
}
