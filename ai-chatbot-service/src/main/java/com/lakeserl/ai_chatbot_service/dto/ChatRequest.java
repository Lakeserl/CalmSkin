package com.lakeserl.ai_chatbot_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChatRequest {
    // Null means start a new conversation
    private Long conversationId;

    @NotBlank
    @Size(max = 2000)
    private String message;
}
