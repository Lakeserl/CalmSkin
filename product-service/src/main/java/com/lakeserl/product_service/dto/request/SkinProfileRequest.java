package com.lakeserl.product_service.dto.request;

import com.lakeserl.product_service.enums.SkinType;
import lombok.Data;

import java.util.List;

@Data
public class SkinProfileRequest {
    private SkinType skinType;
    private List<String> skinConcerns;
    private List<String> allergies;
}
