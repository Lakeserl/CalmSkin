package com.lakeserl.ai_chatbot_service.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class KnowledgeBaseDTO {
    private Long id;
    private String topic;
    private String title;
    private String content;
    private LocalDateTime createdAt;
}
