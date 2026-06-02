package com.lakeserl.search_service.client;

import com.lakeserl.search_service.dto.ApiResponse;
import com.lakeserl.search_service.dto.ProductIndexDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "product-service", path = "/internal/products")
public interface ProductServiceClient {

    @GetMapping("/search-index")
    ApiResponse<List<ProductIndexDTO>> getAllForIndex(
            @RequestHeader("X-Internal-Secret") String secret,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "500") int size);
}
