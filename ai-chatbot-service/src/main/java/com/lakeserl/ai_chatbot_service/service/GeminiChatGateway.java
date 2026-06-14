package com.lakeserl.ai_chatbot_service.service;

import java.util.List;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Component;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;

/**
 * Thin circuit-breaker boundary around the Gemini chat call. Must be its own bean so
 * the resilience4j AOP proxy applies (a same-class call would bypass it). When Gemini
 * is down or the breaker is open the call throws, and ChatServiceImpl's existing
 * catch degrades to a static reply.
 */
@Component
@RequiredArgsConstructor
public class GeminiChatGateway {

    private final ChatClient chatClient;

    @CircuitBreaker(name = "gemini-chat")
    public ChatResponse call(List<Message> messages) {
        return chatClient.prompt()
                .messages(messages)
                .call()
                .chatResponse();
    }
}
