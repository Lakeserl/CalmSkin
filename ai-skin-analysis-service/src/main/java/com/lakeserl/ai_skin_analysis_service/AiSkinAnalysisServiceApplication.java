package com.lakeserl.ai_skin_analysis_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
public class AiSkinAnalysisServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiSkinAnalysisServiceApplication.class, args);
    }
}
