package com.lakeserl.product_service.service;

import com.lakeserl.product_service.dto.request.CreateProductRequest;
import com.lakeserl.product_service.dto.request.CreateVariantRequest;
import com.lakeserl.product_service.dto.request.UpdateProductRequest;
import com.lakeserl.product_service.dto.request.UpdateVariantRequest;
import com.lakeserl.product_service.dto.response.ProductDTO;
import com.lakeserl.product_service.dto.response.ProductStatsDTO;
import com.lakeserl.product_service.dto.response.ProductVariantDTO;
import com.lakeserl.product_service.entity.*;
import com.lakeserl.product_service.enums.ProductStatus;
import com.lakeserl.product_service.exception.DuplicateSkuException;
import com.lakeserl.product_service.exception.DuplicateSlugException;
import com.lakeserl.product_service.exception.InvalidProductStatusException;
import com.lakeserl.product_service.exception.ProductNotFoundException;
import com.lakeserl.product_service.exception.ProductVariantLimitException;
import com.lakeserl.product_service.mapper.ProductMapper;
import com.lakeserl.product_service.mapper.VariantMapper;
import com.lakeserl.product_service.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class AdminProductServiceImpl implements AdminProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;
    private final IngredientRepository ingredientRepository;
    private final TagRepository tagRepository;
    private final ProductVariantRepository variantRepository;
    private final ProductImageRepository productImageRepository;
    private final ProductIngredientRepository productIngredientRepository;
    private final ProductTagRepository productTagRepository;
    
    private final ProductMapper productMapper;
    private final VariantMapper variantMapper;

    private static final Pattern NONLATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]");

    @Override
    @Transactional
    public ProductDTO createProduct(CreateProductRequest request) {
        if (productRepository.existsBySku(request.getSku())) {
            throw new DuplicateSkuException(request.getSku());
        }

        String slug = generateSlug(request.getName());
        if (productRepository.existsBySlug(slug)) {
            slug = slug + "-" + request.getSku().toLowerCase();
            if (productRepository.existsBySlug(slug)) {
                throw new DuplicateSlugException(slug);
            }
        }

        Product product = productMapper.toEntity(request);
        product.setSlug(slug);

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("Category not found"));
        Brand brand = brandRepository.findById(request.getBrandId())
                .orElseThrow(() -> new IllegalArgumentException("Brand not found"));

        product.setCategory(category);
        product.setBrand(brand);
        product.setStatus(ProductStatus.INACTIVE); // Default to inactive

        return productMapper.toDto(productRepository.save(product));
    }

    @Override
    @Transactional
    public ProductDTO updateProduct(Long id, UpdateProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));

        if (request.getSku() != null && !request.getSku().equals(product.getSku())) {
            if (productRepository.existsBySku(request.getSku())) {
                throw new DuplicateSkuException(request.getSku());
            }
        }

        productMapper.updateEntityFromRequest(request, product);

        if (request.getName() != null && !request.getName().equals(product.getName())) {
            String slug = generateSlug(request.getName());
            if (!slug.equals(product.getSlug())) {
                if (productRepository.existsBySlug(slug)) {
                    slug = slug + "-" + product.getSku().toLowerCase();
                }
                product.setSlug(slug);
            }
        }

        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new IllegalArgumentException("Category not found"));
            product.setCategory(category);
        }

        if (request.getBrandId() != null) {
            Brand brand = brandRepository.findById(request.getBrandId())
                    .orElseThrow(() -> new IllegalArgumentException("Brand not found"));
            product.setBrand(brand);
        }

        return productMapper.toDto(productRepository.save(product));
    }

    @Override
    @Transactional
    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
        product.setStatus(ProductStatus.DISCONTINUED);
        productRepository.save(product);
    }

    @Override
    @Transactional
    public ProductDTO updateProductStatus(Long id, ProductStatus status) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
        if (status == ProductStatus.ACTIVE) {
            boolean hasImage = productImageRepository.existsByProductId(id);
            if (!hasImage) {
                throw new InvalidProductStatusException("Cannot activate product without at least one image");
            }

            if (product.getBasePrice() == null
                    || product.getBasePrice().compareTo(java.math.BigDecimal.ZERO) <= 0) {
                throw new InvalidProductStatusException("Cannot activate product with invalid price");
            }

            if (product.getCategory() == null || product.getBrand() == null) {
                throw new InvalidProductStatusException("Cannot activate product without category and brand");
            }
        }
        product.setStatus(status);
        return productMapper.toDto(productRepository.save(product));
    }

    @Override
    @Transactional
    public ProductVariantDTO addVariant(Long productId, CreateVariantRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));

        long variantCount = variantRepository.countByProductId(productId);
        if (variantCount >= 20) {
            throw new ProductVariantLimitException("Product cannot have more than 20 variants");
        }

        if (variantRepository.existsBySku(request.getSku())) {
            throw new DuplicateSkuException(request.getSku());
        }

        ProductVariant variant = variantMapper.toEntity(request);
        variant.setProduct(product);

        if (Boolean.TRUE.equals(request.getIsDefault())) {
            // Remove default from others
            variantRepository.findByProductIdAndIsDefaultTrue(productId).ifPresent(v -> {
                v.setIsDefault(false);
                variantRepository.save(v);
            });
        }

        return variantMapper.toDto(variantRepository.save(variant));
    }

    @Override
    @Transactional
    public ProductVariantDTO updateVariant(Long productId, Long variantId, UpdateVariantRequest request) {
        ProductVariant variant = variantRepository.findById(variantId)
                .orElseThrow(() -> new IllegalArgumentException("Variant not found"));

        if (!variant.getProduct().getId().equals(productId)) {
            throw new IllegalArgumentException("Variant does not belong to product");
        }

        if (request.getSku() != null && !request.getSku().equals(variant.getSku())) {
            if (variantRepository.existsBySku(request.getSku())) {
                throw new DuplicateSkuException(request.getSku());
            }
        }

        variantMapper.updateEntityFromRequest(request, variant);

        if (Boolean.TRUE.equals(request.getIsDefault())) {
            variantRepository.findByProductIdAndIsDefaultTrue(productId).ifPresent(v -> {
                if (!v.getId().equals(variantId)) {
                    v.setIsDefault(false);
                    variantRepository.save(v);
                }
            });
        }

        return variantMapper.toDto(variantRepository.save(variant));
    }

    @Override
    @Transactional
    public void removeVariant(Long productId, Long variantId) {
        ProductVariant variant = variantRepository.findById(variantId)
                .orElseThrow(() -> new IllegalArgumentException("Variant not found"));
                
        if (!variant.getProduct().getId().equals(productId)) {
            throw new IllegalArgumentException("Variant does not belong to product");
        }
        
        variantRepository.delete(variant);
    }

    @Override
    @Transactional
    public ProductDTO linkIngredients(Long productId, List<Long> ingredientIds) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));

        // Clear existing
        List<ProductIngredient> existing = productIngredientRepository.findByProductIdOrderByDisplayOrderAsc(productId);
        productIngredientRepository.deleteAll(existing);

        if (ingredientIds != null && !ingredientIds.isEmpty()) {
            for (int i = 0; i < ingredientIds.size(); i++) {
                Ingredient ingredient = ingredientRepository.findById(ingredientIds.get(i))
                        .orElseThrow(() -> new IllegalArgumentException("Ingredient not found"));
                        
                ProductIngredient pi = ProductIngredient.builder()
                        .product(product)
                        .ingredient(ingredient)
                        .displayOrder(i)
                        .isKeyIngredient(i < 3) // First 3 are key ingredients
                        .build();
                        
                productIngredientRepository.save(pi);
            }
        }
        
        // Refresh product to return updated data
        return productMapper.toDto(productRepository.findById(productId).get());
    }

    @Override
    @Transactional
    public ProductDTO linkTags(Long productId, List<Long> tagIds) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));

        List<ProductTag> existing = productTagRepository.findByProductId(productId);
        productTagRepository.deleteAll(existing);

        if (tagIds != null && !tagIds.isEmpty()) {
            for (Long tagId : tagIds) {
                Tag tag = tagRepository.findById(tagId)
                        .orElseThrow(() -> new IllegalArgumentException("Tag not found"));
                        
                ProductTag pt = ProductTag.builder()
                        .product(product)
                        .tag(tag)
                        .build();
                        
                productTagRepository.save(pt);
            }
        }

        return productMapper.toDto(productRepository.findById(productId).get());
    }

    @Override
    public ProductStatsDTO getProductStats() {
        return ProductStatsDTO.builder()
                .totalProducts(productRepository.count())
                // In a real scenario, use more complex queries for stats
                .build();
    }

    private String generateSlug(String input) {
        String nowhitespace = WHITESPACE.matcher(input).replaceAll("-");
        String normalized = Normalizer.normalize(nowhitespace, Normalizer.Form.NFD);
        String slug = NONLATIN.matcher(normalized).replaceAll("");
        return slug.toLowerCase(Locale.ENGLISH).replaceAll("-{2,}", "-").replaceAll("^-|-$", "");
    }
}
