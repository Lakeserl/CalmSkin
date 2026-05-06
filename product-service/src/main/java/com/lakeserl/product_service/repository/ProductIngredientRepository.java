package com.lakeserl.product_service.repository;

import com.lakeserl.product_service.entity.ProductIngredient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductIngredientRepository extends JpaRepository<ProductIngredient, Long> {

    List<ProductIngredient> findByProductIdOrderByDisplayOrderAsc(Long productId);
    
    List<ProductIngredient> findByIngredientId(Long ingredientId);
    
    boolean existsByProductIdAndIngredientId(Long productId, Long ingredientId);
}
