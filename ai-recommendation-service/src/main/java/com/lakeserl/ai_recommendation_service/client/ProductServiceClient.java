package com.lakeserl.ai_recommendation_service.client;

import com.lakeserl.ai_recommendation_service.dto.ApiResponse;
import com.lakeserl.ai_recommendation_service.dto.ProductDTO;
import com.lakeserl.ai_recommendation_service.dto.UserSkinProfileDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

@FeignClient(name = "product-service", path = "/internal/products")
public interface ProductServiceClient {

    @PostMapping("/by-skin-profile")
    ApiResponse<Map<String, List<ProductDTO>>> getBySkinProfile(
            @RequestBody UserSkinProfileDTO request,
            @RequestHeader("X-Internal-Secret") String secret);

    @GetMapping("/{id}")
    ApiResponse<ProductDTO> getById(
            @PathVariable Long id,
            @RequestHeader("X-Internal-Secret") String secret);

    @GetMapping("/batch")
    ApiResponse<List<ProductDTO>> getBatch(
            @RequestParam List<Long> ids,
            @RequestHeader("X-Internal-Secret") String secret);
}
