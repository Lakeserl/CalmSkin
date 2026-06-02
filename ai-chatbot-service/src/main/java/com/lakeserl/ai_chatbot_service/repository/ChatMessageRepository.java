package com.lakeserl.ai_chatbot_service.repository;

import com.lakeserl.ai_chatbot_service.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    // Most recent N messages for building the conversation context window
    List<ChatMessage> findTop10ByConversationIdOrderByCreatedAtAsc(Long conversationId);

    List<ChatMessage> findByConversationIdOrderByCreatedAtAsc(Long conversationId);
}
