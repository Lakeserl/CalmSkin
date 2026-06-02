package com.lakeserl.ai_chatbot_service.dto;

import lombok.Data;

import java.util.List;

@Data
public class UserSkinProfileDTO {
    private String skinType;
    private List<String> skinConcerns;
    private List<String> allergies;
    private String note;
}
