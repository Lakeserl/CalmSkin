package com.lakeserl.product_service.controller;

import com.lakeserl.product_service.dto.request.CreateBrandRequest;
import com.lakeserl.product_service.dto.request.UpdateBrandRequest;
import com.lakeserl.product_service.dto.response.ApiResponse;
import com.lakeserl.product_service.dto.response.BrandDTO;
import com.lakeserl.product_service.service.BrandService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/brands")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Auth")
@Tag(name = "Admin Brand APIs", description = "Admin endpoints for brand management")
public class AdminBrandController {

    private final BrandService brandService;

    @PostMapping
    @Operation(summary = "Create new brand")
    public ApiResponse<BrandDTO> createBrand(@Valid @RequestBody CreateBrandRequest request) {
        return ApiResponse.ok("Brand created successfully", brandService.createBrand(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update brand")
    public ApiResponse<BrandDTO> updateBrand(@PathVariable Long id, @Valid @RequestBody UpdateBrandRequest request) {
        return ApiResponse.ok("Brand updated successfully", brandService.updateBrand(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete brand (soft delete)")
    public ApiResponse<Void> deleteBrand(@PathVariable Long id) {
        brandService.deleteBrand(id);
        return ApiResponse.ok("Brand deleted successfully");
    }
}
