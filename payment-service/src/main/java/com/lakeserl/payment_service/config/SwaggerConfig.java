package com.lakeserl.payment_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("CalmSkin Payment Service API")
                        .version("1.0.0")
                        .description("Payment lifecycle, gateway webhooks and refunds (sandbox only)")
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
