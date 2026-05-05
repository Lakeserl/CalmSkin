package com.lakeserl.user_service.model.dto.request;

import java.util.List;

import com.lakeserl.user_service.model.enums.SkinType;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SkinProfileRequest {

    @NotNull(message = "Skin type is required")
    private SkinType skinType;

    private List<String> skinConcerns;

    private List<String> allergies;

    private String note;
}
