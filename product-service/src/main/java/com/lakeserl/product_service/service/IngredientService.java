package com.lakeserl.product_service.service;

import java.util.List;

import com.lakeserl.product_service.dto.request.CheckSafeRequest;
import com.lakeserl.product_service.dto.request.CreateIngredientRequest;
import com.lakeserl.product_service.dto.request.UpdateIngredientRequest;
import com.lakeserl.product_service.dto.response.IngredientConflictDTO;
import com.lakeserl.product_service.dto.response.IngredientDTO;
import com.lakeserl.product_service.dto.response.IngredientSafetyDTO;

public interface IngredientService {
    List<IngredientDTO> getAllIngredients();
    IngredientDTO getIngredientById(Long id);
    List<IngredientDTO> getIngredientsByProductId(Long productId);
    
    // Safety & AI Integration
    List<IngredientSafetyDTO> checkIngredientsSafety(CheckSafeRequest request);
    List<IngredientConflictDTO> checkConflicts(List<Long> ingredientIds);
    
    // Admin
    IngredientDTO createIngredient(CreateIngredientRequest request);
    IngredientDTO updateIngredient(Long id, UpdateIngredientRequest request);
    void deleteIngredient(Long id);
}
