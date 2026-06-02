package com.lakeserl.ai_chatbot_service.controller;

import com.lakeserl.ai_chatbot_service.dto.ApiResponse;
import com.lakeserl.ai_chatbot_service.dto.ChatRequest;
import com.lakeserl.ai_chatbot_service.dto.ChatResponse;
import com.lakeserl.ai_chatbot_service.dto.ConversationDTO;
import com.lakeserl.ai_chatbot_service.service.ChatService;
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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ai/chat")
@RequiredArgsConstructor
@Tag(name = "Chatbot", description = "RAG-powered skincare advisor")
public class ChatController {

    private final ChatService chatService;

    @PostMapping
    @Operation(summary = "Send a message to the skincare advisor")
    public ResponseEntity<ApiResponse<ChatResponse>> chat(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody ChatRequest request) {

        ChatResponse response = chatService.chat(request, UUID.fromString(userId));
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/conversations")
    @Operation(summary = "List all conversations for the logged-in user")
    public ResponseEntity<ApiResponse<Page<ConversationDTO>>> getConversations(
            @RequestHeader("X-User-Id") String userId,
            Pageable pageable) {

        return ResponseEntity.ok(ApiResponse.ok(chatService.getConversations(UUID.fromString(userId), pageable)));
    }

    @GetMapping("/conversations/{id}")
    @Operation(summary = "Get a conversation with all messages")
    public ResponseEntity<ApiResponse<ConversationDTO>> getConversation(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable Long id) {

        return ResponseEntity.ok(ApiResponse.ok(chatService.getConversation(id, UUID.fromString(userId))));
    }

    @DeleteMapping("/conversations/{id}")
    @Operation(summary = "Delete a conversation")
    public ResponseEntity<ApiResponse<Void>> deleteConversation(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable Long id) {

        chatService.deleteConversation(id, UUID.fromString(userId));
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
