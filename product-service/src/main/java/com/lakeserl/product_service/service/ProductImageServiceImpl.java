package com.lakeserl.product_service.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.lakeserl.product_service.dto.response.ProductImageDTO;
import com.lakeserl.product_service.entity.Product;
import com.lakeserl.product_service.entity.ProductImage;
import com.lakeserl.product_service.exception.ImageUploadException;
import com.lakeserl.product_service.exception.ProductImageLimitException;
import com.lakeserl.product_service.exception.ProductNotFoundException;
import com.lakeserl.product_service.mapper.ProductMapper;
import com.lakeserl.product_service.repository.ProductImageRepository;
import com.lakeserl.product_service.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductImageServiceImpl implements ProductImageService {

    private final ProductImageRepository imageRepository;
    private final ProductRepository productRepository;
    private final ProductMapper productMapper;
    private final Cloudinary cloudinary;

    private static final int MAX_IMAGES_PER_PRODUCT = 10;

    @Override
    public List<ProductImageDTO> getProductImages(Long productId) {
        return imageRepository.findByProductIdOrderByDisplayOrderAsc(productId)
                .stream()
                .map(productMapper::toImageDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ProductImageDTO uploadImage(Long productId, MultipartFile file, Boolean isPrimary) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));

        long currentImageCount = imageRepository.countByProductId(productId);
        if (currentImageCount >= MAX_IMAGES_PER_PRODUCT) {
            throw new ProductImageLimitException("Product already has the maximum allowed images (" + MAX_IMAGES_PER_PRODUCT + ")");
        }

        try {
            Map uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                    "folder", "calmskin/products",
                    "use_filename", true,
                    "unique_filename", true
            ));

            String url = uploadResult.get("secure_url").toString();

            if (isPrimary != null && isPrimary) {
                // Remove primary flag from other images
                imageRepository.findByProductIdAndIsPrimaryTrue(productId).ifPresent(img -> {
                    img.setIsPrimary(false);
                    imageRepository.save(img);
                });
            } else if (currentImageCount == 0) {
                // First image is always primary
                isPrimary = true;
            } else {
                isPrimary = false;
            }

            ProductImage image = ProductImage.builder()
                    .product(product)
                    .url(url)
                    .altText(product.getName() + " image")
                    .isPrimary(isPrimary)
                    .displayOrder((int) currentImageCount)
                    .build();

            return productMapper.toImageDto(imageRepository.save(image));

        } catch (IOException e) {
            log.error("Failed to upload image to Cloudinary", e);
            throw new ImageUploadException("Failed to upload image", e);
        }
    }

    @Override
    @Transactional
    public ProductImageDTO setPrimaryImage(Long productId, Long imageId) {
        if (!productRepository.existsById(productId)) {
            throw new ProductNotFoundException(productId);
        }

        imageRepository.findByProductIdAndIsPrimaryTrue(productId).ifPresent(img -> {
            if (!img.getId().equals(imageId)) {
                img.setIsPrimary(false);
                imageRepository.save(img);
            }
        });

        ProductImage image = imageRepository.findById(imageId)
                .orElseThrow(() -> new IllegalArgumentException("Image not found: " + imageId));
                
        if (!image.getProduct().getId().equals(productId)) {
            throw new IllegalArgumentException("Image does not belong to this product");
        }

        image.setIsPrimary(true);
        return productMapper.toImageDto(imageRepository.save(image));
    }

    @Override
    @Transactional
    public void deleteImage(Long productId, Long imageId) {
        ProductImage image = imageRepository.findById(imageId)
                .orElseThrow(() -> new IllegalArgumentException("Image not found: " + imageId));
                
        if (!image.getProduct().getId().equals(productId)) {
            throw new IllegalArgumentException("Image does not belong to this product");
        }

        boolean wasPrimary = image.getIsPrimary();
        
        // Extract public ID from Cloudinary URL to delete it from Cloudinary
        // URL format: https://res.cloudinary.com/.../image/upload/v1234567890/folder/filename.ext
        try {
            String url = image.getUrl();
            String[] parts = url.split("/");
            String filenameWithExt = parts[parts.length - 1];
            String folder = parts[parts.length - 2];
            String publicId = folder + "/" + filenameWithExt.substring(0, filenameWithExt.lastIndexOf('.'));
            
            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
        } catch (Exception e) {
            log.warn("Failed to delete image from Cloudinary, continuing with DB deletion", e);
        }

        imageRepository.delete(image);

        if (wasPrimary) {
            // Set the first available image as primary if we just deleted the primary one
            List<ProductImage> remaining = imageRepository.findByProductIdOrderByDisplayOrderAsc(productId);
            if (!remaining.isEmpty()) {
                ProductImage newPrimary = remaining.get(0);
                newPrimary.setIsPrimary(true);
                imageRepository.save(newPrimary);
            }
        }
    }

    @Override
    @Transactional
    public List<ProductImageDTO> reorderImages(Long productId, List<Long> imageIds) {
        if (!productRepository.existsById(productId)) {
            throw new ProductNotFoundException(productId);
        }

        List<ProductImage> images = imageRepository.findByProductIdOrderByDisplayOrderAsc(productId);
        
        for (int i = 0; i < imageIds.size(); i++) {
            Long id = imageIds.get(i);
            int finalI = i;
            images.stream()
                    .filter(img -> img.getId().equals(id))
                    .findFirst()
                    .ifPresent(img -> {
                        img.setDisplayOrder(finalI);
                        imageRepository.save(img);
                    });
        }

        return imageRepository.findByProductIdOrderByDisplayOrderAsc(productId)
                .stream()
                .map(productMapper::toImageDto)
                .collect(Collectors.toList());
    }
}
