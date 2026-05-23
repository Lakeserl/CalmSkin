package com.lakeserl.review_service.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI reviewServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("CalmSKIN Review Service API")
                        .description("Product review management: ratings, media, votes, replies, reports, and aggregated summaries.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("CalmSKIN Engineering")
                                .email("lakeserl010@gmail.com")))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Gateway (local)"),
                        new Server().url("http://localhost:8091").description("Direct (local dev)")
                ));
    }
}
