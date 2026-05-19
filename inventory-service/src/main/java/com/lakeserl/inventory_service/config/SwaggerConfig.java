package com.lakeserl.inventory_service.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("CalmSkin Inventory Service API")
                        .version("1.0.0")
                        .description("Inventory stock management and reservation APIs")
                        .contact(new Contact().name("CalmSkin Team")))
                .addSecurityItem(new SecurityRequirement().addList("Bearer Auth"))
                .addSecurityItem(new SecurityRequirement().addList("Internal Secret"))
                .components(new Components()
                        .addSecuritySchemes("Bearer Auth",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Enter your JWT access token"))
                        .addSecuritySchemes("Internal Secret",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.APIKEY)
                                        .in(SecurityScheme.In.HEADER)
                                        .name("X-Internal-Secret")
                                        .description("Secret key for internal service-to-service communication")));
    }
}
