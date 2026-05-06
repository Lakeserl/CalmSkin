package com.lakeserl.product_service.controller;

import com.lakeserl.product_service.dto.request.CreateProductRequest;
import com.lakeserl.product_service.dto.request.CreateVariantRequest;
import com.lakeserl.product_service.dto.request.UpdateProductRequest;
import com.lakeserl.product_service.dto.request.UpdateVariantRequest;
import com.lakeserl.product_service.dto.response.ApiResponse;
import com.lakeserl.product_service.dto.response.ProductDTO;
import com.lakeserl.product_service.dto.response.ProductImageDTO;
import com.lakeserl.product_service.dto.response.ProductStatsDTO;
import com.lakeserl.product_service.dto.response.ProductVariantDTO;
import com.lakeserl.product_service.enums.ProductStatus;
import com.lakeserl.product_service.service.AdminProductService;
import com.lakeserl.product_service.service.ProductImageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/products")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Auth")
@Tag(name = "Admin Product APIs", description = "Admin endpoints for product catalog management")
public class AdminProductController {

    private final AdminProductService adminProductService;
    private final ProductImageService productImageService;

    // --- Product Core ---

    @PostMapping
    @Operation(summary = "Create a new product")
    public ApiResponse<ProductDTO> createProduct(@Valid @RequestBody CreateProductRequest request) {
        return ApiResponse.ok("Product created successfully", adminProductService.createProduct(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing product")
    public ApiResponse<ProductDTO> updateProduct(@PathVariable Long id, @Valid @RequestBody UpdateProductRequest request) {
        return ApiResponse.ok("Product updated successfully", adminProductService.updateProduct(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft delete a product")
    public ApiResponse<Void> deleteProduct(@PathVariable Long id) {
        adminProductService.deleteProduct(id);
        return ApiResponse.ok("Product deleted successfully");
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update product status")
    public ApiResponse<ProductDTO> updateProductStatus(@PathVariable Long id, @RequestParam ProductStatus status) {
        return ApiResponse.ok("Status updated", adminProductService.updateProductStatus(id, status));
    }

    // --- Variants ---

    @PostMapping("/{id}/variants")
    @Operation(summary = "Add a variant to a product")
    public ApiResponse<ProductVariantDTO> addVariant(@PathVariable Long id, @Valid @RequestBody CreateVariantRequest request) {
        return ApiResponse.ok("Variant added successfully", adminProductService.addVariant(id, request));
    }

    @PutMapping("/{id}/variants/{variantId}")
    @Operation(summary = "Update a product variant")
    public ApiResponse<ProductVariantDTO> updateVariant(
            @PathVariable Long id, 
            @PathVariable Long variantId, 
            @Valid @RequestBody UpdateVariantRequest request) {
        return ApiResponse.ok("Variant updated successfully", adminProductService.updateVariant(id, variantId, request));
    }

    @DeleteMapping("/{id}/variants/{variantId}")
    @Operation(summary = "Remove a variant from a product")
    public ApiResponse<Void> removeVariant(@PathVariable Long id, @PathVariable Long variantId) {
        adminProductService.removeVariant(id, variantId);
        return ApiResponse.ok("Variant removed successfully");
    }

    // --- Images ---

    @PostMapping(value = "/{id}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload a product image")
    public ApiResponse<ProductImageDTO> uploadImage(
            @PathVariable Long id, 
            @RequestPart("file") MultipartFile file, 
            @RequestParam(required = false) Boolean isPrimary) {
        return ApiResponse.ok("Image uploaded successfully", productImageService.uploadImage(id, file, isPrimary));
    }

    @PutMapping("/{id}/images/{imageId}/primary")
    @Operation(summary = "Set an image as primary")
    public ApiResponse<ProductImageDTO> setPrimaryImage(@PathVariable Long id, @PathVariable Long imageId) {
        return ApiResponse.ok("Primary image updated", productImageService.setPrimaryImage(id, imageId));
    }

    @DeleteMapping("/{id}/images/{imageId}")
    @Operation(summary = "Delete an image")
    public ApiResponse<Void> deleteImage(@PathVariable Long id, @PathVariable Long imageId) {
        productImageService.deleteImage(id, imageId);
        return ApiResponse.ok("Image deleted successfully");
    }

    @PutMapping("/{id}/images/reorder")
    @Operation(summary = "Reorder images")
    public ApiResponse<List<ProductImageDTO>> reorderImages(@PathVariable Long id, @RequestBody List<Long> imageIds) {
        return ApiResponse.ok("Images reordered", productImageService.reorderImages(id, imageIds));
    }

    // --- Links ---

    @PutMapping("/{id}/ingredients")
    @Operation(summary = "Link ingredients to a product")
    public ApiResponse<ProductDTO> linkIngredients(@PathVariable Long id, @RequestBody List<Long> ingredientIds) {
        return ApiResponse.ok("Ingredients updated", adminProductService.linkIngredients(id, ingredientIds));
    }

    @PutMapping("/{id}/tags")
    @Operation(summary = "Link tags to a product")
    public ApiResponse<ProductDTO> linkTags(@PathVariable Long id, @RequestBody List<Long> tagIds) {
        return ApiResponse.ok("Tags updated", adminProductService.linkTags(id, tagIds));
    }

    // --- Stats ---

    @GetMapping("/stats")
    @Operation(summary = "Get product statistics for dashboard")
    public ApiResponse<ProductStatsDTO> getStats() {
        return ApiResponse.ok(adminProductService.getProductStats());
    }
}
