package com.lakeserl.product_service.service;

import com.lakeserl.product_service.dto.request.CreateProductRequest;
import com.lakeserl.product_service.dto.request.CreateVariantRequest;
import com.lakeserl.product_service.dto.request.UpdateProductRequest;
import com.lakeserl.product_service.dto.request.UpdateVariantRequest;
import com.lakeserl.product_service.dto.response.ProductDTO;
import com.lakeserl.product_service.dto.response.ProductStatsDTO;
import com.lakeserl.product_service.dto.response.ProductVariantDTO;
import com.lakeserl.product_service.enums.ProductStatus;

import java.util.List;

public interface AdminProductService {
    ProductDTO createProduct(CreateProductRequest request);
    ProductDTO updateProduct(Long id, UpdateProductRequest request);
    void deleteProduct(Long id);
    ProductDTO updateProductStatus(Long id, ProductStatus status);
    
    // Variant management
    ProductVariantDTO addVariant(Long productId, CreateVariantRequest request);
    ProductVariantDTO updateVariant(Long productId, Long variantId, UpdateVariantRequest request);
    void removeVariant(Long productId, Long variantId);
    
    // Ingredients & Tags
    ProductDTO linkIngredients(Long productId, List<Long> ingredientIds);
    ProductDTO linkTags(Long productId, List<Long> tagIds);
    
    // Dashboard Stats
    ProductStatsDTO getProductStats();
}
