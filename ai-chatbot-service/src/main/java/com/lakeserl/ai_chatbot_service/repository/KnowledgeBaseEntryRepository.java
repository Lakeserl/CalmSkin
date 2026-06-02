package com.lakeserl.ai_chatbot_service.repository;

import com.lakeserl.ai_chatbot_service.entity.KnowledgeBaseEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface KnowledgeBaseEntryRepository extends JpaRepository<KnowledgeBaseEntry, Long> {

    Page<KnowledgeBaseEntry> findByTopicOrderByCreatedAtDesc(String topic, Pageable pageable);

    Optional<KnowledgeBaseEntry> findByVectorStoreId(String vectorStoreId);
}
