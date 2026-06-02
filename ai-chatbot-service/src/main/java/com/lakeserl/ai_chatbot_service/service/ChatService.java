package com.lakeserl.ai_chatbot_service.service;

import com.lakeserl.ai_chatbot_service.dto.ChatRequest;
import com.lakeserl.ai_chatbot_service.dto.ChatResponse;
import com.lakeserl.ai_chatbot_service.dto.ConversationDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ChatService {

    ChatResponse chat(ChatRequest request, UUID userId);

    Page<ConversationDTO> getConversations(UUID userId, Pageable pageable);

    ConversationDTO getConversation(Long conversationId, UUID userId);

    void deleteConversation(Long conversationId, UUID userId);
}
