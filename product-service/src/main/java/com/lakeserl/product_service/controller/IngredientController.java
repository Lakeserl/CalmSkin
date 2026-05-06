package com.lakeserl.product_service.controller;

import com.lakeserl.product_service.dto.request.CheckSafeRequest;
import com.lakeserl.product_service.dto.response.ApiResponse;
import com.lakeserl.product_service.dto.response.IngredientConflictDTO;
import com.lakeserl.product_service.dto.response.IngredientDTO;
import com.lakeserl.product_service.dto.response.IngredientSafetyDTO;
import com.lakeserl.product_service.service.IngredientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ingredients")
@RequiredArgsConstructor
@Tag(name = "Ingredient APIs", description = "Endpoints for viewing ingredients and safety checks")
public class IngredientController {

    private final IngredientService ingredientService;

    @GetMapping
    @Operation(summary = "Get all ingredients")
    public ApiResponse<List<IngredientDTO>> getAllIngredients() {
        return ApiResponse.ok(ingredientService.getAllIngredients());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get ingredient by ID")
    public ApiResponse<IngredientDTO> getIngredientById(@PathVariable Long id) {
        return ApiResponse.ok(ingredientService.getIngredientById(id));
    }

    @PostMapping("/check-safety")
    @Operation(summary = "Check safety of a list of ingredients against a skin type")
    public ApiResponse<List<IngredientSafetyDTO>> checkSafety(@Valid @RequestBody CheckSafeRequest request) {
        return ApiResponse.ok(ingredientService.checkIngredientsSafety(request));
    }

    @PostMapping("/check-conflicts")
    @Operation(summary = "Check for conflicts among a list of ingredients")
    public ApiResponse<List<IngredientConflictDTO>> checkConflicts(@RequestBody List<Long> ingredientIds) {
        return ApiResponse.ok(ingredientService.checkConflicts(ingredientIds));
    }
}
