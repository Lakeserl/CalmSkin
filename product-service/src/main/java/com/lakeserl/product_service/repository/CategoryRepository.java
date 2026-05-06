package com.lakeserl.product_service.repository;

import com.lakeserl.product_service.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findBySlug(String slug);
    
    boolean existsBySlug(String slug);

    List<Category> findByParentIsNullOrderByDisplayOrderAsc();
    
    List<Category> findByParentIsNullAndIsActiveTrueOrderByDisplayOrderAsc();

    List<Category> findByIsActiveTrueOrderByDisplayOrderAsc();
}
