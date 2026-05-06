package com.lakeserl.product_service.controller;

import com.lakeserl.product_service.dto.request.CreateCategoryRequest;
import com.lakeserl.product_service.dto.request.UpdateCategoryRequest;
import com.lakeserl.product_service.dto.response.ApiResponse;
import com.lakeserl.product_service.dto.response.CategoryDTO;
import com.lakeserl.product_service.dto.response.CategoryTreeDTO;
import com.lakeserl.product_service.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
@Tag(name = "Category APIs", description = "Endpoints for category management")
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    @Operation(summary = "Get all active categories (flat list)")
    public ApiResponse<List<CategoryDTO>> getAllCategories() {
        return ApiResponse.ok(categoryService.getAllCategories());
    }

    @GetMapping("/tree")
    @Operation(summary = "Get active categories as a hierarchical tree")
    public ApiResponse<List<CategoryTreeDTO>> getCategoryTree() {
        return ApiResponse.ok(categoryService.getCategoryTree());
    }

    @GetMapping("/{slug}")
    @Operation(summary = "Get category by slug")
    public ApiResponse<CategoryDTO> getCategoryBySlug(@PathVariable String slug) {
        return ApiResponse.ok(categoryService.getCategoryBySlug(slug));
    }

    // Admin Endpoints for Category are mixed in for simplicity or could be separated.
    // Given the task, we are placing Admin specific ones in AdminProductController,
    // but category/brand admin might just be here protected by security config.
    // Based on SecurityConfig, /api/v1/admin/** is for admin.
    // To match SecurityConfig we will prefix admin routes. Let's create an AdminCategoryController or put them in AdminProductController.
    // For now, I'll put admin methods in a separate path mapping inside this file for simplicity, 
    // or just leave them mapped under /api/v1/admin/categories.
}
