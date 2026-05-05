package com.lakeserl.user_service.model.dto.response;

import java.util.List;
import java.util.UUID;

import com.lakeserl.user_service.model.enums.SkinType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkinProfileResponse {
    private UUID id;
    private SkinType skinType;
    private List<String> skinConcerns;
    private List<String> allergies;
    private String note;
}
