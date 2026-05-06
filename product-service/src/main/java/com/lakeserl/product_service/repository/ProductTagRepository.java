package com.lakeserl.product_service.repository;

import com.lakeserl.product_service.entity.ProductTag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductTagRepository extends JpaRepository<ProductTag, Long> {

    List<ProductTag> findByProductId(Long productId);
    
    List<ProductTag> findByTagId(Long tagId);
    
    boolean existsByProductIdAndTagId(Long productId, Long tagId);
}
