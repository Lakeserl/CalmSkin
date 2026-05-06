package com.lakeserl.product_service.service;

import com.lakeserl.product_service.dto.request.CreateCategoryRequest;
import com.lakeserl.product_service.dto.request.UpdateCategoryRequest;
import com.lakeserl.product_service.dto.response.CategoryDTO;
import com.lakeserl.product_service.dto.response.CategoryTreeDTO;

import java.util.List;

public interface CategoryService {
    List<CategoryDTO> getAllCategories();
    List<CategoryTreeDTO> getCategoryTree();
    CategoryDTO getCategoryBySlug(String slug);
    
    // Admin methods
    CategoryDTO createCategory(CreateCategoryRequest request);
    CategoryDTO updateCategory(Long id, UpdateCategoryRequest request);
    void deleteCategory(Long id);
    CategoryDTO updateCategoryStatus(Long id, Boolean isActive);
}
