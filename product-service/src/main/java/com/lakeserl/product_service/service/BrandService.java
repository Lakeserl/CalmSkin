package com.lakeserl.product_service.service;

import com.lakeserl.product_service.dto.request.CreateBrandRequest;
import com.lakeserl.product_service.dto.request.UpdateBrandRequest;
import com.lakeserl.product_service.dto.response.BrandDTO;

import java.util.List;

public interface BrandService {
    List<BrandDTO> getAllBrands();
    BrandDTO getBrandBySlug(String slug);
    
    // Admin methods
    BrandDTO createBrand(CreateBrandRequest request);
    BrandDTO updateBrand(Long id, UpdateBrandRequest request);
    void deleteBrand(Long id);
}
