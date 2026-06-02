package com.lakeserl.ai_recommendation_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class AiRecommendationServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(AiRecommendationServiceApplication.class, args);
    }
}
