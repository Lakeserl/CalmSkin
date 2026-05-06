package com.lakeserl.product_service.mapper;

import com.lakeserl.product_service.dto.request.CreateCategoryRequest;
import com.lakeserl.product_service.dto.request.UpdateCategoryRequest;
import com.lakeserl.product_service.dto.response.CategoryDTO;
import com.lakeserl.product_service.dto.response.CategoryTreeDTO;
import com.lakeserl.product_service.entity.Category;
import org.mapstruct.*;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface CategoryMapper {

    @Mapping(target = "parentId", source = "parent.id")
    @Mapping(target = "parentName", source = "parent.name")
    CategoryDTO toDto(Category category);

    CategoryTreeDTO toTreeDto(Category category);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "parent", ignore = true)
    @Mapping(target = "subCategories", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "slug", ignore = true)
    Category toEntity(CreateCategoryRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "parent", ignore = true)
    @Mapping(target = "subCategories", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "slug", ignore = true)
    void updateEntityFromRequest(UpdateCategoryRequest request, @MappingTarget Category category);
}
