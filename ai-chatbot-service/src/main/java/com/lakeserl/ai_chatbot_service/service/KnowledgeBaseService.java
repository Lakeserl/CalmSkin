package com.lakeserl.ai_chatbot_service.service;

import com.lakeserl.ai_chatbot_service.dto.KnowledgeBaseDTO;
import com.lakeserl.ai_chatbot_service.dto.KnowledgeBaseRequest;
import com.lakeserl.ai_chatbot_service.entity.KnowledgeBaseEntry;
import com.lakeserl.ai_chatbot_service.repository.KnowledgeBaseEntryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeBaseService {

    private final VectorStore vectorStore;
    private final KnowledgeBaseEntryRepository entryRepository;

    @Transactional
    public KnowledgeBaseDTO addEntry(KnowledgeBaseRequest request) {
        String docId = UUID.randomUUID().toString();

        Document doc = new Document(
                docId,
                request.getContent(),
                Map.of("topic", request.getTopic(), "title", request.getTitle())
        );
        vectorStore.add(List.of(doc));

        KnowledgeBaseEntry entry = KnowledgeBaseEntry.builder()
                .vectorStoreId(docId)
                .topic(request.getTopic())
                .title(request.getTitle())
                .content(request.getContent())
                .createdAt(LocalDateTime.now())
                .build();
        entryRepository.save(entry);

        log.info("Added knowledge base entry: topic={}, title={}", request.getTopic(), request.getTitle());

        return toDto(entry);
    }

    @Transactional
    public void deleteEntry(Long entryId) {
        KnowledgeBaseEntry entry = entryRepository.findById(entryId)
                .orElseThrow(() -> new IllegalArgumentException("Knowledge base entry not found: " + entryId));

        vectorStore.delete(List.of(entry.getVectorStoreId()));
        entryRepository.delete(entry);

        log.info("Deleted knowledge base entry id={}, vectorStoreId={}", entryId, entry.getVectorStoreId());
    }

    @Transactional(readOnly = true)
    public Page<KnowledgeBaseDTO> list(String topic, Pageable pageable) {
        Page<KnowledgeBaseEntry> page = topic != null && !topic.isBlank()
                ? entryRepository.findByTopicOrderByCreatedAtDesc(topic, pageable)
                : entryRepository.findAll(pageable);
        return page.map(this::toDto);
    }

    private KnowledgeBaseDTO toDto(KnowledgeBaseEntry e) {
        return KnowledgeBaseDTO.builder()
                .id(e.getId())
                .topic(e.getTopic())
                .title(e.getTitle())
                .content(e.getContent())
                .createdAt(e.getCreatedAt())
                .build();
    }
}
