package com.lakeserl.product_service.repository;

import com.lakeserl.product_service.entity.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {

    List<ProductVariant> findByProductId(Long productId);
    
    List<ProductVariant> findByProductIdAndIsActiveTrue(Long productId);
    
    Optional<ProductVariant> findBySku(String sku);
    
    boolean existsBySku(String sku);
    
    Optional<ProductVariant> findByProductIdAndIsDefaultTrue(Long productId);
}
