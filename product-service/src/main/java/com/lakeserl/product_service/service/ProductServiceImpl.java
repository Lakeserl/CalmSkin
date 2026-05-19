package com.lakeserl.product_service.service;

import com.lakeserl.product_service.dto.request.ProductFilterRequest;
import com.lakeserl.product_service.dto.request.ProductValidationRequest;
import com.lakeserl.product_service.dto.request.SkinProfileRequest;
import com.lakeserl.product_service.dto.response.ProductDTO;
import com.lakeserl.product_service.dto.response.ProductInternalDTO;
import com.lakeserl.product_service.dto.response.ProductSummaryDTO;
import com.lakeserl.product_service.dto.response.ProductValidationResult;
import com.lakeserl.product_service.entity.Product;
import com.lakeserl.product_service.enums.ProductStatus;
import com.lakeserl.product_service.exception.ProductNotActiveException;
import com.lakeserl.product_service.exception.ProductNotFoundException;
import com.lakeserl.product_service.mapper.ProductMapper;
import com.lakeserl.product_service.repository.ProductRepository;
import com.lakeserl.product_service.specification.ProductSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String VIEW_COUNT_KEY_PREFIX = "product:view:";

    @Override
    @Transactional(readOnly = true)
    public Page<ProductSummaryDTO> searchProducts(ProductFilterRequest filter, Pageable pageable) {
        Specification<Product> spec = Specification
                .where(ProductSpecification.hasStatus(ProductStatus.ACTIVE));

        if (filter.getQ() != null && !filter.getQ().isEmpty()) {
            spec = spec.and(ProductSpecification.search(filter.getQ()));
        }
        if (filter.getCategory() != null) {
            spec = spec.and(ProductSpecification.hasCategorySlug(filter.getCategory()));
        }
        if (filter.getBrand() != null) {
            spec = spec.and(ProductSpecification.hasBrandSlug(filter.getBrand()));
        }
        if (filter.getSkinType() != null) {
            spec = spec.and(ProductSpecification.hasSkinType(filter.getSkinType()));
        }
        if (filter.getSkinConcern() != null) {
            spec = spec.and(ProductSpecification.hasSkinConcern(filter.getSkinConcern()));
        }
        if (filter.getMinPrice() != null) {
            spec = spec.and(ProductSpecification.priceGreaterThanOrEqualTo(filter.getMinPrice()));
        }
        if (filter.getMaxPrice() != null) {
            spec = spec.and(ProductSpecification.priceLessThanOrEqualTo(filter.getMaxPrice()));
        }
        if (filter.getTag() != null) {
            spec = spec.and(ProductSpecification.hasTagSlug(filter.getTag()));
        }
        if (filter.getUsageStep() != null) {
            spec = spec.and(ProductSpecification.hasUsageStep(filter.getUsageStep()));
        }
        if (filter.getIsFeatured() != null) {
            spec = spec.and(ProductSpecification.isFeatured(filter.getIsFeatured()));
        }
        if (filter.getIsNew() != null) {
            spec = spec.and(ProductSpecification.isNewArrival(filter.getIsNew()));
        }

        Pageable sortedPageable = applySorting(filter.getSort(), pageable);

        return productRepository.findAll(spec, sortedPageable).map(productMapper::toSummaryDto);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductDTO getProductBySlug(String slug) {
        Product product = productRepository.findBySlug(slug)
                .orElseThrow(() -> new ProductNotFoundException("Product not found with slug: " + slug));

        if (product.getStatus() != ProductStatus.ACTIVE) {
            throw new ProductNotActiveException("Product is not active: " + slug);
        }

        return productMapper.toDto(product);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductDTO getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
        return productMapper.toDto(product);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductSummaryDTO> getBestSellers(Pageable pageable) {
        return productRepository.findBestSellers(pageable).map(productMapper::toSummaryDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductSummaryDTO> getNewArrivals(Pageable pageable) {
        return productRepository.findByIsNewArrivalTrueAndStatus(ProductStatus.ACTIVE, pageable)
                .map(productMapper::toSummaryDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductSummaryDTO> getSimilarProducts(String slug, int limit) {
        Product product = productRepository.findBySlug(slug)
                .orElseThrow(() -> new ProductNotFoundException("Product not found with slug: " + slug));

        List<Product> similar = productRepository.findSimilarProducts(
                product.getCategory() != null ? product.getCategory().getId() : null,
                product.getBrand() != null ? product.getBrand().getId() : null,
                product.getUsageStep() != null ? product.getUsageStep().name() : null,
                product.getId(),
                limit
        );

        return similar.stream().map(productMapper::toSummaryDto).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductInternalDTO> getProductsByIds(List<Long> ids) {
        return productRepository.findAllById(ids).stream()
                .map(this::toInternalDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductValidationResult> validateProducts(List<ProductValidationRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            return List.of();
        }

        return requests.stream()
                .map(req -> {
                    ProductValidationResult.ProductValidationResultBuilder builder = ProductValidationResult.builder()
                            .productId(req.getProductId());

                    if (req.getProductId() == null) {
                        return builder.valid(false).reason("MISSING_PRODUCT_ID").build();
                    }

                    Product product = productRepository.findById(req.getProductId()).orElse(null);
                    if (product == null) {
                        return builder.valid(false).reason("PRODUCT_NOT_FOUND").build();
                    }

                    if (product.getStatus() != ProductStatus.ACTIVE) {
                        return builder.valid(false).reason("PRODUCT_NOT_ACTIVE").build();
                    }

                    return builder.valid(true).reason("OK").build();
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, List<ProductSummaryDTO>> findBySkinProfile(SkinProfileRequest request) {
        if (request == null) {
            return Map.of();
        }

        var spec = Specification.where(ProductSpecification.hasStatus(ProductStatus.ACTIVE));

        if (request.getSkinType() != null) {
            spec = spec.and(ProductSpecification.hasSkinType(request.getSkinType().name()));
        }

        if (request.getSkinConcerns() != null) {
            for (String concern : request.getSkinConcerns()) {
                spec = spec.and(ProductSpecification.hasSkinConcern(concern));
            }
        }

        List<Product> products = productRepository.findAll(spec);

        if (request.getAllergies() != null && !request.getAllergies().isEmpty()) {
            Set<String> allergySet = request.getAllergies().stream()
                    .map(String::toLowerCase)
                    .collect(Collectors.toSet());

            products = products.stream()
                    .filter(product -> product.getProductIngredients() == null
                            || product.getProductIngredients().stream().noneMatch(pi -> {
                                if (pi.getIngredient() == null || pi.getIngredient().getName() == null) {
                                    return false;
                                }
                                return allergySet.contains(pi.getIngredient().getName().toLowerCase());
                            }))
                    .collect(Collectors.toList());
        }

        Map<String, List<ProductSummaryDTO>> grouped = new LinkedHashMap<>();
        for (Product product : products) {
            String key = product.getUsageStep() != null ? product.getUsageStep().name() : "OTHER";
            grouped.computeIfAbsent(key, k -> new java.util.ArrayList<>())
                    .add(productMapper.toSummaryDto(product));
        }
        return grouped;
    }

    @Override
    public void recordProductView(Long productId) {
        if (productId != null) {
            String key = VIEW_COUNT_KEY_PREFIX + productId;
            redisTemplate.opsForValue().increment(key);
        }
    }

    private ProductInternalDTO toInternalDto(Product product) {
        return ProductInternalDTO.builder()
                .id(product.getId())
                .name(product.getName())
                .slug(product.getSlug())
                .sku(product.getSku())
                .status(product.getStatus())
                .basePrice(product.getBasePrice())
                .salePrice(product.getSalePrice())
                .brandId(product.getBrand() != null ? product.getBrand().getId() : null)
                .categoryId(product.getCategory() != null ? product.getCategory().getId() : null)
                .build();
    }

    private Pageable applySorting(String sortParam, Pageable pageable) {
        if (sortParam == null || sortParam.isEmpty()) {
            return pageable;
        }

        Sort sort = switch (sortParam) {
            case "price_asc" -> Sort.by(Sort.Direction.ASC, "salePrice", "basePrice");
            case "price_desc" -> Sort.by(Sort.Direction.DESC, "salePrice", "basePrice");
            case "newest" -> Sort.by(Sort.Direction.DESC, "createdAt");
            case "best_seller" -> Sort.by(Sort.Direction.DESC, "soldCount");
            case "view_count" -> Sort.by(Sort.Direction.DESC, "viewCount");
            default -> Sort.unsorted();
        };

        if (sort.isUnsorted()) {
            return pageable;
        }

        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
    }
}
