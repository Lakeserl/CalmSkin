package com.lakeserl.ai_chatbot_service.repository;

import com.lakeserl.ai_chatbot_service.entity.ChatConversation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ChatConversationRepository extends JpaRepository<ChatConversation, Long> {

    Page<ChatConversation> findByUserIdOrderByUpdatedAtDesc(UUID userId, Pageable pageable);

    Optional<ChatConversation> findByIdAndUserId(Long id, UUID userId);
}
