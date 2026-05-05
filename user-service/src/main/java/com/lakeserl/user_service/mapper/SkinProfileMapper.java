package com.lakeserl.user_service.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lakeserl.user_service.model.dto.request.SkinProfileRequest;
import com.lakeserl.user_service.model.dto.response.SkinProfileResponse;
import com.lakeserl.user_service.model.entity.SkinProfile;

@Mapper(componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface SkinProfileMapper {

    @Mapping(target = "skinConcerns", expression = "java(jsonToList(profile.getSkinConcerns()))")
    @Mapping(target = "allergies", expression = "java(jsonToList(profile.getAllergies()))")
    SkinProfileResponse toResponse(SkinProfile profile);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "skinConcerns", expression = "java(listToJson(request.getSkinConcerns()))")
    @Mapping(target = "allergies", expression = "java(listToJson(request.getAllergies()))")
    SkinProfile toEntity(SkinProfileRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "skinConcerns", expression = "java(listToJson(request.getSkinConcerns()))")
    @Mapping(target = "allergies", expression = "java(listToJson(request.getAllergies()))")
    void updateFromRequest(SkinProfileRequest request, @MappingTarget SkinProfile profile);

    default String listToJson(List<String> list) {
        if (list == null) return null;
        try {
            return new ObjectMapper().writeValueAsString(list);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }

    default List<String> jsonToList(String json) {
        if (json == null) return List.of();
        try {
            return new ObjectMapper().readValue(json, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
            return List.of();
        }
    }
}
