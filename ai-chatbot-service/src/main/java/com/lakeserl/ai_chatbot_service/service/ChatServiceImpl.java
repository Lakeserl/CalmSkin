package com.lakeserl.ai_chatbot_service.service;

import com.lakeserl.ai_chatbot_service.dto.ChatRequest;
import com.lakeserl.ai_chatbot_service.dto.ChatResponse;
import com.lakeserl.ai_chatbot_service.dto.ConversationDTO;
import com.lakeserl.ai_chatbot_service.entity.ChatConversation;
import com.lakeserl.ai_chatbot_service.entity.ChatMessage;
import com.lakeserl.ai_chatbot_service.enums.MessageRole;
import com.lakeserl.ai_chatbot_service.exception.ConversationNotFoundException;
import com.lakeserl.ai_chatbot_service.exception.DailyLimitExceededException;
import com.lakeserl.ai_chatbot_service.repository.ChatConversationRepository;
import com.lakeserl.ai_chatbot_service.repository.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final GeminiChatGateway geminiChatGateway;
    private final VectorStore vectorStore;
    private final ChatConversationRepository conversationRepository;
    private final ChatMessageRepository messageRepository;
    private final UserContextService userContextService;
    private final RedisTemplate<String, String> redisTemplate;
    private final AIUsageLogService usageLogService;

    @Value("${app.ai.daily-limit-chatbot:50}")
    private int dailyLimit;

    @Value("${spring.ai.google.genai.chat.options.model:gemini-1.5-flash}")
    private String modelName;

    @Value("classpath:prompts/chatbot-system.txt")
    private org.springframework.core.io.Resource systemPromptResource;

    @Override
    @Transactional
    public ChatResponse chat(ChatRequest request, UUID userId) {
        enforceRateLimit(userId);

        ChatConversation conversation = resolveConversation(request.getConversationId(), userId);

        // Fetch last 10 messages for context window
        List<ChatMessage> history = messageRepository
                .findTop10ByConversationIdOrderByCreatedAtAsc(conversation.getId());

        // RAG: embed user message and retrieve top-5 relevant knowledge chunks
        List<Document> relevantDocs = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(request.getMessage())
                        .topK(5)
                        .build());

        String knowledgeContext = relevantDocs.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n---\n"));

        // User skin profile for personalized advice
        String userContext = userContextService.buildUserContext(userId);

        String systemPrompt = loadSystemPrompt(knowledgeContext, userContext);

        // Build message list: system + history + current user message
        List<Message> messages = buildMessages(systemPrompt, history, request.getMessage());

        String assistantReply;
        int tokensUsed = 0;
        long aiStart = System.currentTimeMillis();
        try {
            var response = geminiChatGateway.call(messages);

            assistantReply = response.getResult().getOutput().getText();
            int tokensInput = 0;
            int tokensOutput = 0;
            if (response.getMetadata() != null && response.getMetadata().getUsage() != null) {
                var usage = response.getMetadata().getUsage();
                tokensInput = usage.getPromptTokens() != null ? usage.getPromptTokens() : 0;
                tokensOutput = usage.getCompletionTokens() != null ? usage.getCompletionTokens() : 0;
                tokensUsed = tokensInput + tokensOutput;
            }
            usageLogService.record(userId, modelName, tokensInput, tokensOutput,
                    (int) (System.currentTimeMillis() - aiStart), true, null);
        } catch (Exception e) {
            log.error("Gemini chat call failed for conversationId={}: {}", conversation.getId(), e.getMessage());
            assistantReply = "Xin lỗi, hiện tại tôi không thể trả lời. Vui lòng thử lại sau.";
            usageLogService.record(userId, modelName, 0, 0,
                    (int) (System.currentTimeMillis() - aiStart), false, e.getMessage());
        }

        // Persist both turns
        persistMessage(conversation.getId(), MessageRole.USER, request.getMessage(), 0);
        persistMessage(conversation.getId(), MessageRole.ASSISTANT, assistantReply, tokensUsed);

        // Update conversation title on first real exchange
        if (conversation.getTitle() == null && !request.getMessage().isBlank()) {
            String title = request.getMessage().length() > 80
                    ? request.getMessage().substring(0, 80) + "…"
                    : request.getMessage();
            conversation.setTitle(title);
        }
        conversation.setUpdatedAt(LocalDateTime.now());
        conversationRepository.save(conversation);

        List<String> suggestedActions = parseSuggestedActions(assistantReply);

        return ChatResponse.builder()
                .conversationId(conversation.getId())
                .response(assistantReply)
                .suggestedActions(suggestedActions)
                .tokensUsed(tokensUsed)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ConversationDTO> getConversations(UUID userId, Pageable pageable) {
        return conversationRepository.findByUserIdOrderByUpdatedAtDesc(userId, pageable)
                .map(c -> ConversationDTO.builder()
                        .id(c.getId())
                        .title(c.getTitle())
                        .createdAt(c.getCreatedAt())
                        .updatedAt(c.getUpdatedAt())
                        .build());
    }

    @Override
    @Transactional(readOnly = true)
    public ConversationDTO getConversation(Long conversationId, UUID userId) {
        ChatConversation conv = conversationRepository.findByIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new ConversationNotFoundException("Conversation not found: " + conversationId));

        List<ChatMessage> msgs = messageRepository.findByConversationIdOrderByCreatedAtAsc(conv.getId());
        List<ConversationDTO.MessageDTO> messageDtos = msgs.stream()
                .map(m -> ConversationDTO.MessageDTO.builder()
                        .role(m.getRole().name())
                        .content(m.getContent())
                        .createdAt(m.getCreatedAt())
                        .build())
                .collect(Collectors.toList());

        return ConversationDTO.builder()
                .id(conv.getId())
                .title(conv.getTitle())
                .createdAt(conv.getCreatedAt())
                .updatedAt(conv.getUpdatedAt())
                .messages(messageDtos)
                .build();
    }

    @Override
    @Transactional
    public void deleteConversation(Long conversationId, UUID userId) {
        ChatConversation conv = conversationRepository.findByIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new ConversationNotFoundException("Conversation not found: " + conversationId));
        conversationRepository.delete(conv);
        log.info("Deleted conversation id={} for userId={}", conversationId, userId);
    }

    private void enforceRateLimit(UUID userId) {
        String today = LocalDate.now().toString();
        String limitKey = "chat:limit:" + userId + ":" + today;
        Long count = redisTemplate.opsForValue().increment(limitKey);
        if (count == 1L) {
            // Expire at end of day
            LocalDateTime endOfDay = LocalDate.now().atTime(23, 59, 59);
            long epochSeconds = endOfDay.atZone(ZoneId.systemDefault()).toEpochSecond();
            redisTemplate.expireAt(limitKey, new java.util.Date(epochSeconds * 1000));
        }
        if (count != null && count > dailyLimit) {
            throw new DailyLimitExceededException(
                    "Daily chat limit of " + dailyLimit + " messages reached. Try again tomorrow.");
        }
    }

    private ChatConversation resolveConversation(Long conversationId, UUID userId) {
        if (conversationId != null) {
            return conversationRepository.findByIdAndUserId(conversationId, userId)
                    .orElseThrow(() -> new ConversationNotFoundException(
                            "Conversation not found: " + conversationId));
        }
        ChatConversation newConv = ChatConversation.builder()
                .userId(userId)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        return conversationRepository.save(newConv);
    }

    private String loadSystemPrompt(String knowledgeContext, String userContext) {
        try {
            String template = new String(systemPromptResource.getInputStream().readAllBytes());
            return template
                    .replace("{{KNOWLEDGE_CONTEXT}}", knowledgeContext.isBlank() ? "No specific knowledge available." : knowledgeContext)
                    .replace("{{USER_CONTEXT}}", userContext.isBlank() ? "No skin profile on file." : userContext);
        } catch (Exception e) {
            log.warn("Could not load system prompt template: {}", e.getMessage());
            return "You are a helpful skincare advisor for CalmSKIN. Answer questions about skincare in Vietnamese.";
        }
    }

    private List<Message> buildMessages(String systemPrompt, List<ChatMessage> history, String userMessage) {
        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(systemPrompt));
        for (ChatMessage msg : history) {
            if (msg.getRole() == MessageRole.USER) {
                messages.add(new UserMessage(msg.getContent()));
            } else if (msg.getRole() == MessageRole.ASSISTANT) {
                messages.add(new AssistantMessage(msg.getContent()));
            }
        }
        messages.add(new UserMessage(userMessage));
        return messages;
    }

    private void persistMessage(Long conversationId, MessageRole role, String content, int tokens) {
        messageRepository.save(ChatMessage.builder()
                .conversationId(conversationId)
                .role(role)
                .content(content)
                .tokensUsed(tokens > 0 ? tokens : null)
                .createdAt(LocalDateTime.now())
                .build());
    }

    // Looks for suggested actions in the response, e.g. lines starting with "ACTION:"
    private List<String> parseSuggestedActions(String response) {
        List<String> actions = new ArrayList<>();
        if (response == null) return actions;
        for (String line : response.split("\n")) {
            if (line.startsWith("ACTION:")) {
                actions.add(line.substring("ACTION:".length()).trim());
            }
        }
        return actions;
    }
}
