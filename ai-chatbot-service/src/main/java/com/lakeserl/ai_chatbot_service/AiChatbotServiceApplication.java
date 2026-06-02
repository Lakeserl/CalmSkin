package com.lakeserl.ai_chatbot_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class AiChatbotServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(AiChatbotServiceApplication.class, args);
    }
}
