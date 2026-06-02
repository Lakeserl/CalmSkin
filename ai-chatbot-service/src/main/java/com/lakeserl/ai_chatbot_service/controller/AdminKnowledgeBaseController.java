package com.lakeserl.ai_chatbot_service.controller;

import com.lakeserl.ai_chatbot_service.dto.ApiResponse;
import com.lakeserl.ai_chatbot_service.dto.KnowledgeBaseDTO;
import com.lakeserl.ai_chatbot_service.dto.KnowledgeBaseRequest;
import com.lakeserl.ai_chatbot_service.service.KnowledgeBaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/ai/chat/knowledge-base")
@RequiredArgsConstructor
@Tag(name = "Admin - Knowledge Base", description = "Manage the RAG knowledge base")
public class AdminKnowledgeBaseController {

    private final KnowledgeBaseService knowledgeBaseService;

    @PostMapping
    @Operation(summary = "Add a knowledge base entry (will be embedded automatically)")
    public ResponseEntity<ApiResponse<KnowledgeBaseDTO>> addEntry(
            @Valid @RequestBody KnowledgeBaseRequest request) {

        return ResponseEntity.ok(ApiResponse.ok(knowledgeBaseService.addEntry(request)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a knowledge base entry (removes from vector store)")
    public ResponseEntity<ApiResponse<Void>> deleteEntry(@PathVariable Long id) {
        knowledgeBaseService.deleteEntry(id);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @GetMapping
    @Operation(summary = "List knowledge base entries (optionally filtered by topic)")
    public ResponseEntity<ApiResponse<Page<KnowledgeBaseDTO>>> list(
            @RequestParam(required = false) String topic,
            Pageable pageable) {

        return ResponseEntity.ok(ApiResponse.ok(knowledgeBaseService.list(topic, pageable)));
    }
}
