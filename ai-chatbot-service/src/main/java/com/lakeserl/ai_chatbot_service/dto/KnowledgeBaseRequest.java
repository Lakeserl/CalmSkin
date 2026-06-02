package com.lakeserl.ai_chatbot_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class KnowledgeBaseRequest {

    @NotBlank
    @Size(max = 50)
    private String topic;   // INGREDIENT, ROUTINE, FAQ, PRODUCT

    @NotBlank
    @Size(max = 255)
    private String title;

    @NotBlank
    @Size(max = 10000)
    private String content;
}
