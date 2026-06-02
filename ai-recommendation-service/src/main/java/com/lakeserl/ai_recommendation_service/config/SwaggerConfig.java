package com.lakeserl.ai_recommendation_service.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("CalmSkin AI Recommendation Service")
                        .description("Skin-profile-based and content-based product recommendations")
                        .version("1.0.0"));
    }
}
