package com.lakeserl.product_service.controller;

import com.lakeserl.product_service.dto.request.ProductFilterRequest;
import com.lakeserl.product_service.dto.response.ApiResponse;
import com.lakeserl.product_service.dto.response.ProductDTO;
import com.lakeserl.product_service.dto.response.ProductSummaryDTO;
import com.lakeserl.product_service.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@Tag(name = "Public Product APIs", description = "Endpoints for viewing products, search, and filtering")
public class ProductController {

    private final ProductService productService;

    @GetMapping
    @Operation(summary = "Search and filter products")
    public ApiResponse<Page<ProductSummaryDTO>> searchProducts(@ModelAttribute ProductFilterRequest filter, Pageable pageable) {
        return ApiResponse.ok(productService.searchProducts(filter, pageable));
    }

    @GetMapping("/{slug}")
    @Operation(summary = "Get product details by slug")
    public ApiResponse<ProductDTO> getProductBySlug(@PathVariable String slug) {
        ProductDTO product = productService.getProductBySlug(slug);
        productService.recordProductView(product.getId());
        return ApiResponse.ok(product);
    }

    @GetMapping("/best-sellers")
    @Operation(summary = "Get best selling products")
    public ApiResponse<Page<ProductSummaryDTO>> getBestSellers(Pageable pageable) {
        return ApiResponse.ok(productService.getBestSellers(pageable));
    }

    @GetMapping("/new-arrivals")
    @Operation(summary = "Get new arrival products")
    public ApiResponse<Page<ProductSummaryDTO>> getNewArrivals(Pageable pageable) {
        return ApiResponse.ok(productService.getNewArrivals(pageable));
    }

    @GetMapping("/{slug}/similar")
    @Operation(summary = "Get similar products")
    public ApiResponse<List<ProductSummaryDTO>> getSimilarProducts(
            @PathVariable String slug,
            @RequestParam(defaultValue = "4") int limit) {
        return ApiResponse.ok(productService.getSimilarProducts(slug, limit));
    }

    @GetMapping("/compare")
    @Operation(summary = "Compare 2-4 products side-by-side")
    public ApiResponse<List<ProductDTO>> compareProducts(@RequestParam List<Long> ids) {
        return ApiResponse.ok(productService.compareProducts(ids));
    }
}
