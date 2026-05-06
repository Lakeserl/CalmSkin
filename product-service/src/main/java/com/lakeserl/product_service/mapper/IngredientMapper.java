package com.lakeserl.product_service.mapper;

import com.lakeserl.product_service.dto.request.CreateIngredientRequest;
import com.lakeserl.product_service.dto.request.UpdateIngredientRequest;
import com.lakeserl.product_service.dto.response.IngredientDTO;
import com.lakeserl.product_service.entity.Ingredient;
import org.mapstruct.*;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface IngredientMapper {

    IngredientDTO toDto(Ingredient ingredient);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Ingredient toEntity(CreateIngredientRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromRequest(UpdateIngredientRequest request, @MappingTarget Ingredient ingredient);
}
