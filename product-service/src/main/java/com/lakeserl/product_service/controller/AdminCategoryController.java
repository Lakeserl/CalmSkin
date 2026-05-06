package com.lakeserl.product_service.controller;

import com.lakeserl.product_service.dto.request.CreateCategoryRequest;
import com.lakeserl.product_service.dto.request.UpdateCategoryRequest;
import com.lakeserl.product_service.dto.response.ApiResponse;
import com.lakeserl.product_service.dto.response.CategoryDTO;
import com.lakeserl.product_service.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/categories")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Auth")
@Tag(name = "Admin Category APIs", description = "Admin endpoints for category management")
public class AdminCategoryController {

    private final CategoryService categoryService;

    @PostMapping
    @Operation(summary = "Create new category")
    public ApiResponse<CategoryDTO> createCategory(@Valid @RequestBody CreateCategoryRequest request) {
        return ApiResponse.ok("Category created successfully", categoryService.createCategory(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update category")
    public ApiResponse<CategoryDTO> updateCategory(@PathVariable Long id, @Valid @RequestBody UpdateCategoryRequest request) {
        return ApiResponse.ok("Category updated successfully", categoryService.updateCategory(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete category (soft delete)")
    public ApiResponse<Void> deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return ApiResponse.ok("Category deleted successfully");
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update category status")
    public ApiResponse<CategoryDTO> updateCategoryStatus(@PathVariable Long id, @RequestParam Boolean isActive) {
        return ApiResponse.ok("Category status updated", categoryService.updateCategoryStatus(id, isActive));
    }
}
