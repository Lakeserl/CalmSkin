package com.lakeserl.product_service.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import com.lakeserl.product_service.dto.request.ProductValidationRequest;
import com.lakeserl.product_service.dto.request.SkinProfileRequest;
import com.lakeserl.product_service.dto.request.UpdateReviewSummaryRequest;
import com.lakeserl.product_service.dto.response.ApiResponse;
import com.lakeserl.product_service.dto.response.IngredientDTO;
import com.lakeserl.product_service.dto.response.ProductDTO;
import com.lakeserl.product_service.dto.response.ProductInternalDTO;
import com.lakeserl.product_service.dto.response.ProductSummaryDTO;
import com.lakeserl.product_service.dto.response.ProductValidationResult;
import com.lakeserl.product_service.service.IngredientService;
import com.lakeserl.product_service.service.ProductService;
import com.lakeserl.product_service.service.ReviewSummaryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/products")
@RequiredArgsConstructor
@SecurityRequirement(name = "Internal Secret")
@Tag(name = "Internal Product APIs", description = "Endpoints for service-to-service communication")
public class InternalProductController {

    private final ProductService productService;
    private final IngredientService ingredientService;
    private final ReviewSummaryService reviewSummaryService;

    @GetMapping("/{id}")
    @Operation(summary = "Get product by ID (For other microservices like Order or Cart)")
    public ApiResponse<ProductDTO> getProductById(@PathVariable Long id) {
        return ApiResponse.ok(productService.getProductById(id));
    }

    @GetMapping("/batch")
    @Operation(summary = "Get products by IDs")
    public ApiResponse<List<ProductInternalDTO>> getProductsBatch(@RequestParam List<Long> ids) {
        return ApiResponse.ok(productService.getProductsByIds(ids));
    }

    @PostMapping("/validate")
    @Operation(summary = "Validate products for internal services")
    public ApiResponse<List<ProductValidationResult>> validateProducts(
            @RequestBody List<ProductValidationRequest> requests) {
        return ApiResponse.ok(productService.validateProducts(requests));
    }

    @GetMapping("/{id}/ingredients")
    @Operation(summary = "Get ingredients by product")
    public ApiResponse<List<IngredientDTO>> getProductIngredients(@PathVariable Long id) {
        return ApiResponse.ok(ingredientService.getIngredientsByProductId(id));
    }

    @PostMapping("/{id}/increment-view")
    @Operation(summary = "Increment view count")
    public ResponseEntity<Void> incrementView(@PathVariable Long id) {
        productService.recordProductView(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/review-summary")
    @Operation(summary = "Update review summary")
    public ResponseEntity<Void> updateReviewSummary(@RequestBody UpdateReviewSummaryRequest request) {
        reviewSummaryService.handleReviewSummaryUpdate(request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/by-skin-profile")
    @Operation(summary = "Get products by skin profile")
    public ApiResponse<Map<String, List<ProductSummaryDTO>>> getBySkinProfile(
            @RequestBody SkinProfileRequest request) {
        return ApiResponse.ok(productService.findBySkinProfile(request));
    }
}
