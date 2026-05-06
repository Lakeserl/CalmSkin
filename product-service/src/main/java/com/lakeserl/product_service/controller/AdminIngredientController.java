package com.lakeserl.product_service.controller;

import com.lakeserl.product_service.dto.request.CreateIngredientRequest;
import com.lakeserl.product_service.dto.request.UpdateIngredientRequest;
import com.lakeserl.product_service.dto.response.ApiResponse;
import com.lakeserl.product_service.dto.response.IngredientDTO;
import com.lakeserl.product_service.service.IngredientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/ingredients")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Auth")
@Tag(name = "Admin Ingredient APIs", description = "Admin endpoints for ingredient management")
public class AdminIngredientController {

    private final IngredientService ingredientService;

    @PostMapping
    @Operation(summary = "Create new ingredient")
    public ApiResponse<IngredientDTO> createIngredient(@Valid @RequestBody CreateIngredientRequest request) {
        return ApiResponse.ok("Ingredient created successfully", ingredientService.createIngredient(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update ingredient")
    public ApiResponse<IngredientDTO> updateIngredient(@PathVariable Long id, @Valid @RequestBody UpdateIngredientRequest request) {
        return ApiResponse.ok("Ingredient updated successfully", ingredientService.updateIngredient(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete ingredient")
    public ApiResponse<Void> deleteIngredient(@PathVariable Long id) {
        ingredientService.deleteIngredient(id);
        return ApiResponse.ok("Ingredient deleted successfully");
    }
}
