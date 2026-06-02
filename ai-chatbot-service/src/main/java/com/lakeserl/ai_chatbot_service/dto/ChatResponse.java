package com.lakeserl.ai_chatbot_service.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ChatResponse {
    private Long conversationId;
    private String response;
    private List<String> suggestedActions;
    private Integer tokensUsed;
}
