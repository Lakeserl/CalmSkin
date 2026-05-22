package com.lakeserl.promotion_service.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.lakeserl.promotion_service.dto.response.ApiResponse;
import com.lakeserl.promotion_service.dto.response.FlashSaleView;
import com.lakeserl.promotion_service.service.FlashSaleService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/** Public flash-sale browsing. */
@RestController
@RequestMapping("/api/v1/promotions/flash-sales")
@RequiredArgsConstructor
@Tag(name = "Flash sales", description = "Public flash-sale browsing")
public class FlashSaleController {

    private final FlashSaleService flashSaleService;

    @GetMapping("/current")
    @Operation(summary = "Flash sales running now")
    public ApiResponse<List<FlashSaleView>> current() {
        return ApiResponse.ok(flashSaleService.currentFlashSales());
    }

    @GetMapping("/upcoming")
    @Operation(summary = "Flash sales scheduled to start later")
    public ApiResponse<List<FlashSaleView>> upcoming() {
        return ApiResponse.ok(flashSaleService.upcomingFlashSales());
    }
}
