package com.lakeserl.product_service.service;

import com.lakeserl.product_service.dto.request.ProductFilterRequest;
import com.lakeserl.product_service.dto.request.ProductValidationRequest;
import com.lakeserl.product_service.dto.request.SkinProfileRequest;
import com.lakeserl.product_service.dto.response.ProductInternalDTO;
import com.lakeserl.product_service.dto.response.ProductValidationResult;
import com.lakeserl.product_service.dto.response.ProductDTO;
import com.lakeserl.product_service.dto.response.ProductSummaryDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

public interface ProductService {
    Page<ProductSummaryDTO> searchProducts(ProductFilterRequest filter, Pageable pageable);
    
    ProductDTO getProductBySlug(String slug);
    
    ProductDTO getProductById(Long id);
    
    Page<ProductSummaryDTO> getBestSellers(Pageable pageable);
    
    Page<ProductSummaryDTO> getNewArrivals(Pageable pageable);
    
    List<ProductSummaryDTO> getSimilarProducts(String slug, int limit);

    List<ProductInternalDTO> getProductsByIds(List<Long> ids);

    List<ProductValidationResult> validateProducts(List<ProductValidationRequest> requests);

    Map<String, List<ProductSummaryDTO>> findBySkinProfile(SkinProfileRequest request);
    
    void recordProductView(Long productId);
}
