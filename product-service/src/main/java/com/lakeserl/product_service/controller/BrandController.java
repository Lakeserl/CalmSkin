package com.lakeserl.product_service.controller;

import com.lakeserl.product_service.dto.response.ApiResponse;
import com.lakeserl.product_service.dto.response.BrandDTO;
import com.lakeserl.product_service.service.BrandService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/brands")
@RequiredArgsConstructor
@Tag(name = "Brand APIs", description = "Endpoints for viewing brands")
public class BrandController {

    private final BrandService brandService;

    @GetMapping
    @Operation(summary = "Get all active brands")
    public ApiResponse<List<BrandDTO>> getAllBrands() {
        return ApiResponse.ok(brandService.getAllBrands());
    }

    @GetMapping("/{slug}")
    @Operation(summary = "Get brand by slug")
    public ApiResponse<BrandDTO> getBrandBySlug(@PathVariable String slug) {
        return ApiResponse.ok(brandService.getBrandBySlug(slug));
    }
}
