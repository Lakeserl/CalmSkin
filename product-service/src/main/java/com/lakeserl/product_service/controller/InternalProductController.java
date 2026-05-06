package com.lakeserl.product_service.controller;

import com.lakeserl.product_service.dto.response.ApiResponse;
import com.lakeserl.product_service.dto.response.ProductDTO;
import com.lakeserl.product_service.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/products")
@RequiredArgsConstructor
@SecurityRequirement(name = "Internal Secret")
@Tag(name = "Internal Product APIs", description = "Endpoints for service-to-service communication")
public class InternalProductController {

    private final ProductService productService;

    @GetMapping("/{id}")
    @Operation(summary = "Get product by ID (For other microservices like Order or Cart)")
    public ApiResponse<ProductDTO> getProductById(@PathVariable Long id) {
        return ApiResponse.ok(productService.getProductById(id));
    }
}
