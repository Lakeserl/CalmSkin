package com.lakeserl.product_service.mapper;

import com.lakeserl.product_service.dto.request.CreateBrandRequest;
import com.lakeserl.product_service.dto.request.UpdateBrandRequest;
import com.lakeserl.product_service.dto.response.BrandDTO;
import com.lakeserl.product_service.entity.Brand;
import org.mapstruct.*;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface BrandMapper {

    BrandDTO toDto(Brand brand);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "slug", ignore = true)
    Brand toEntity(CreateBrandRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "slug", ignore = true)
    void updateEntityFromRequest(UpdateBrandRequest request, @MappingTarget Brand brand);
}
