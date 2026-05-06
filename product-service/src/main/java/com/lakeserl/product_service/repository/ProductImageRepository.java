package com.lakeserl.product_service.repository;

import com.lakeserl.product_service.entity.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {

    List<ProductImage> findByProductIdOrderByDisplayOrderAsc(Long productId);
    
    long countByProductId(Long productId);
    
    Optional<ProductImage> findByProductIdAndIsPrimaryTrue(Long productId);
}
