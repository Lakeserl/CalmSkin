package com.lakeserl.ai_chatbot_service.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ConversationDTO {
    private Long id;
    private String title;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<MessageDTO> messages;

    @Data
    @Builder
    public static class MessageDTO {
        private String role;
        private String content;
        private LocalDateTime createdAt;
    }
}
