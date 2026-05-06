package com.lakeserl.product_service.mapper;

import com.lakeserl.product_service.dto.request.CreateProductRequest;
import com.lakeserl.product_service.dto.request.UpdateProductRequest;
import com.lakeserl.product_service.dto.response.*;
import com.lakeserl.product_service.entity.Product;
import com.lakeserl.product_service.entity.ProductImage;
import com.lakeserl.product_service.entity.ProductIngredient;
import com.lakeserl.product_service.entity.ProductTag;
import org.mapstruct.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring", 
        uses = {CategoryMapper.class, BrandMapper.class, VariantMapper.class},
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface ProductMapper {

    @Mapping(target = "discountPercent", expression = "java(calculateDiscount(product.getBasePrice(), product.getSalePrice()))")
    @Mapping(target = "keyIngredients", expression = "java(mapKeyIngredients(product.getProductIngredients()))")
    @Mapping(target = "tags", expression = "java(mapTags(product.getProductTags()))")
    ProductDTO toDto(Product product);

    @Mapping(target = "categoryName", source = "category.name")
    @Mapping(target = "brandName", source = "brand.name")
    @Mapping(target = "price", expression = "java(calculateDisplayPrice(product))")
    @Mapping(target = "originalPrice", expression = "java(calculateOriginalPrice(product))")
    @Mapping(target = "discountPercent", expression = "java(calculateDisplayDiscount(product))")
    @Mapping(target = "primaryImageUrl", expression = "java(getPrimaryImageUrl(product.getImages()))")
    @Mapping(target = "tags", expression = "java(mapTags(product.getProductTags()))")
    @Mapping(target = "averageRating", source = "reviewSummary.averageRating")
    @Mapping(target = "totalReviews", source = "reviewSummary.totalReviews")
    ProductSummaryDTO toSummaryDto(Product product);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "slug", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "brand", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "viewCount", ignore = true)
    @Mapping(target = "soldCount", ignore = true)
    @Mapping(target = "variants", ignore = true)
    @Mapping(target = "images", ignore = true)
    @Mapping(target = "productIngredients", ignore = true)
    @Mapping(target = "productTags", ignore = true)
    @Mapping(target = "reviewSummary", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Product toEntity(CreateProductRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "slug", ignore = true)
    @Mapping(target = "sku", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "brand", ignore = true)
    @Mapping(target = "viewCount", ignore = true)
    @Mapping(target = "soldCount", ignore = true)
    @Mapping(target = "variants", ignore = true)
    @Mapping(target = "images", ignore = true)
    @Mapping(target = "productIngredients", ignore = true)
    @Mapping(target = "productTags", ignore = true)
    @Mapping(target = "reviewSummary", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromRequest(UpdateProductRequest request, @MappingTarget Product product);

    ProductImageDTO toImageDto(ProductImage image);

    default BigDecimal calculateDiscount(BigDecimal basePrice, BigDecimal salePrice) {
        if (basePrice == null || salePrice == null || basePrice.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        if (salePrice.compareTo(basePrice) >= 0) {
            return BigDecimal.ZERO;
        }
        return basePrice.subtract(salePrice)
                .divide(basePrice, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"))
                .setScale(0, RoundingMode.HALF_UP);
    }

    default List<IngredientDTO> mapKeyIngredients(List<ProductIngredient> productIngredients) {
        if (productIngredients == null) return null;
        return productIngredients.stream()
                .filter(ProductIngredient::getIsKeyIngredient)
                .map(pi -> {
                    IngredientDTO dto = new IngredientDTO();
                    dto.setId(pi.getIngredient().getId());
                    dto.setName(pi.getIngredient().getName());
                    dto.setInciName(pi.getIngredient().getInciName());
                    return dto;
                })
                .collect(Collectors.toList());
    }

    default List<String> mapTags(List<ProductTag> productTags) {
        if (productTags == null) return null;
        return productTags.stream()
                .map(pt -> pt.getTag().getName())
                .collect(Collectors.toList());
    }

    default String getPrimaryImageUrl(List<ProductImage> images) {
        if (images == null || images.isEmpty()) return null;
        return images.stream()
                .filter(ProductImage::getIsPrimary)
                .map(ProductImage::getUrl)
                .findFirst()
                .orElse(images.get(0).getUrl());
    }

    default BigDecimal calculateDisplayPrice(Product product) {
        if (product.getVariants() != null && !product.getVariants().isEmpty()) {
            return product.getVariants().stream()
                    .filter(v -> Boolean.TRUE.equals(v.getIsDefault()) && Boolean.TRUE.equals(v.getIsActive()))
                    .findFirst()
                    .map(v -> v.getSalePrice() != null ? v.getSalePrice() : v.getPrice())
                    .orElse(product.getSalePrice() != null ? product.getSalePrice() : product.getBasePrice());
        }
        return product.getSalePrice() != null ? product.getSalePrice() : product.getBasePrice();
    }

    default BigDecimal calculateOriginalPrice(Product product) {
        if (product.getVariants() != null && !product.getVariants().isEmpty()) {
            return product.getVariants().stream()
                    .filter(v -> Boolean.TRUE.equals(v.getIsDefault()) && Boolean.TRUE.equals(v.getIsActive()))
                    .findFirst()
                    .map(v -> v.getSalePrice() != null ? v.getPrice() : null)
                    .orElse(product.getSalePrice() != null ? product.getBasePrice() : null);
        }
        return product.getSalePrice() != null ? product.getBasePrice() : null;
    }

    default BigDecimal calculateDisplayDiscount(Product product) {
        if (product.getVariants() != null && !product.getVariants().isEmpty()) {
            return product.getVariants().stream()
                    .filter(v -> Boolean.TRUE.equals(v.getIsDefault()) && Boolean.TRUE.equals(v.getIsActive()))
                    .findFirst()
                    .map(v -> calculateDiscount(v.getPrice(), v.getSalePrice()))
                    .orElse(calculateDiscount(product.getBasePrice(), product.getSalePrice()));
        }
        return calculateDiscount(product.getBasePrice(), product.getSalePrice());
    }
}
