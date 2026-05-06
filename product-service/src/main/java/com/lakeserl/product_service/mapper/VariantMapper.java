package com.lakeserl.product_service.mapper;

import com.lakeserl.product_service.dto.request.CreateVariantRequest;
import com.lakeserl.product_service.dto.request.UpdateVariantRequest;
import com.lakeserl.product_service.dto.response.ProductVariantDTO;
import com.lakeserl.product_service.entity.ProductVariant;
import org.mapstruct.*;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface VariantMapper {

    @Mapping(target = "discountPercent", expression = "java(calculateDiscount(variant.getPrice(), variant.getSalePrice()))")
    ProductVariantDTO toDto(ProductVariant variant);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "product", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    ProductVariant toEntity(CreateVariantRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "product", ignore = true)
    @Mapping(target = "sku", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromRequest(UpdateVariantRequest request, @MappingTarget ProductVariant variant);

    default BigDecimal calculateDiscount(BigDecimal price, BigDecimal salePrice) {
        if (price == null || salePrice == null || price.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        if (salePrice.compareTo(price) >= 0) {
            return BigDecimal.ZERO;
        }
        return price.subtract(salePrice)
                .divide(price, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"))
                .setScale(0, RoundingMode.HALF_UP);
    }
}
