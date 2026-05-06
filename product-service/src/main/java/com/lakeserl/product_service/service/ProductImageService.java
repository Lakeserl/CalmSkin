package com.lakeserl.product_service.service;

import com.lakeserl.product_service.dto.response.ProductImageDTO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ProductImageService {
    List<ProductImageDTO> getProductImages(Long productId);
    ProductImageDTO uploadImage(Long productId, MultipartFile file, Boolean isPrimary);
    ProductImageDTO setPrimaryImage(Long productId, Long imageId);
    void deleteImage(Long productId, Long imageId);
    List<ProductImageDTO> reorderImages(Long productId, List<Long> imageIds);
}
