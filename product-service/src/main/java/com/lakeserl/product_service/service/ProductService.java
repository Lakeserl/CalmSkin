package com.lakeserl.product_service.service;

import com.lakeserl.product_service.dto.request.ProductFilterRequest;
import com.lakeserl.product_service.dto.response.ProductDTO;
import com.lakeserl.product_service.dto.response.ProductSummaryDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ProductService {
    Page<ProductSummaryDTO> searchProducts(ProductFilterRequest filter, Pageable pageable);
    
    ProductDTO getProductBySlug(String slug);
    
    ProductDTO getProductById(Long id);
    
    Page<ProductSummaryDTO> getBestSellers(Pageable pageable);
    
    Page<ProductSummaryDTO> getNewArrivals(Pageable pageable);
    
    List<ProductSummaryDTO> getSimilarProducts(String slug, int limit);
    
    void recordProductView(Long productId);
}
